#!/usr/bin/env bash
#
# Runs every check for the ticketing service and saves the reports in reports/.
#
#   Stage 1  Gradle: JUnit/Mockito unit tests, REST Assured API tests, JaCoCo
#            coverage minimum, Checkstyle, and SpotBugs (./gradlew clean check).
#   Stage 2  Live API tests: starts PostgreSQL and the service, loads seed.sql,
#            and runs the Postman collection with Newman.
#   Stage 3  Copies all reports to reports/ and writes reports/summary.txt.
#
# Usage: bash scripts/run-all-tests.sh [--skip-live]
#
#   --skip-live   Run only stage 1 (no Docker or Node.js needed).
#
# Stage 2 needs Docker (running) and Node.js (for npx). Extra Gradle arguments can
# be passed with the GRADLE_ARGS environment variable.

set -uo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORTS_DIR="$ROOT_DIR/reports"
COLLECTION="$ROOT_DIR/postman/ticketing.postman_collection.json"
SEED_FILE="$ROOT_DIR/src/main/resources/db/seed.sql"
APP_PORT=8080
APP_LOG="$ROOT_DIR/build/app-under-test.log"
NEWMAN_VERSION=6

skip_live=false
for arg in "$@"; do
  case "$arg" in
    --skip-live) skip_live=true ;;
    -h|--help) sed -n '3,17p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown option: $arg (try --help)" >&2; exit 2 ;;
  esac
done

cd "$ROOT_DIR" || exit 1

gradle_status="NOT RUN"
live_status="SKIPPED"
app_pid=""
started_postgres=false

log() { printf '\n==> %s\n' "$*"; }

# Stops the service and any PostgreSQL container this script started.
cleanup() {
  if [[ -n "$app_pid" ]] && kill -0 "$app_pid" 2>/dev/null; then
    kill "$app_pid" 2>/dev/null
    wait "$app_pid" 2>/dev/null
  fi
  if [[ "$started_postgres" == true ]]; then
    docker compose stop postgres >/dev/null 2>&1
  fi
}
trap cleanup EXIT

# Stage 1: everything Gradle runs, against the in-memory H2 test database.
log "Stage 1: unit tests, REST Assured API tests, coverage, Checkstyle, SpotBugs"
# shellcheck disable=SC2086 # GRADLE_ARGS is intentionally split into arguments.
if ./gradlew --console=plain --continue ${GRADLE_ARGS:-} clean check bootJar; then
  gradle_status="PASSED"
else
  gradle_status="FAILED"
fi

# Stage 2: the Postman collection against the real service and PostgreSQL.
run_live_tests() {
  if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
    echo "Docker is not installed or not running. Start Docker, or use --skip-live." >&2
    return 1
  fi
  if ! command -v npx >/dev/null 2>&1; then
    echo "npx was not found. Install Node.js, or use --skip-live." >&2
    return 1
  fi
  if curl -s -o /dev/null "http://localhost:$APP_PORT"; then
    echo "Port $APP_PORT is already in use. Stop the other server and retry." >&2
    return 1
  fi

  local jar
  jar="$(find build/libs -name '*.jar' ! -name '*-plain.jar' 2>/dev/null | head -n 1)"
  if [[ -z "$jar" ]]; then
    echo "No executable JAR in build/libs; stage 1 must build it first." >&2
    return 1
  fi

  if [[ -z "$(docker compose ps --status running -q postgres 2>/dev/null)" ]]; then
    started_postgres=true
  fi
  echo "Starting PostgreSQL..."
  docker compose up -d --wait postgres || return 1

  # The service creates the tables on startup, so seed only after it is up.
  echo "Starting the service (log: build/app-under-test.log)..."
  java -jar "$jar" >"$APP_LOG" 2>&1 &
  app_pid=$!

  local waited=0
  until curl -s -o /dev/null "http://localhost:$APP_PORT/v1/events"; do
    if ! kill -0 "$app_pid" 2>/dev/null || ((waited >= 90)); then
      echo "The service did not start; see build/app-under-test.log." >&2
      return 1
    fi
    sleep 1
    ((waited++))
  done

  echo "Loading seed data..."
  docker compose exec -T postgres psql -q -U ticketing -d ticketing \
    -v ON_ERROR_STOP=1 <"$SEED_FILE" || return 1

  mkdir -p "$REPORTS_DIR/postman"
  npx --yes "newman@$NEWMAN_VERSION" run "$COLLECTION" \
    --env-var "baseUrl=http://localhost:$APP_PORT" \
    --reporters cli,junit \
    --reporter-junit-export "$REPORTS_DIR/postman/newman-results.xml"
}

if [[ "$skip_live" == false ]]; then
  log "Stage 2: Postman collection against the running service"
  if run_live_tests; then
    live_status="PASSED"
  else
    live_status="FAILED"
  fi
fi

# Stage 3: keep reports in the repository, where graders can see them.
log "Stage 3: copying reports to reports/"
mkdir -p "$REPORTS_DIR"
copy_report() {
  if [[ -e "$1" ]]; then
    rm -rf "$2"
    mkdir -p "$(dirname "$2")"
    cp -R "$1" "$2"
  fi
}
copy_report build/reports/tests/test "$REPORTS_DIR/tests"
copy_report build/reports/jacoco/test/html "$REPORTS_DIR/coverage"
copy_report build/reports/jacoco/test/jacocoTestReport.xml "$REPORTS_DIR/coverage/jacoco.xml"
copy_report build/reports/checkstyle "$REPORTS_DIR/checkstyle"
copy_report build/reports/spotbugs "$REPORTS_DIR/spotbugs"

# Prints "covered/total (percent%)" for a JaCoCo counter type, e.g. LINE or BRANCH.
coverage() {
  local xml="$REPORTS_DIR/coverage/jacoco.xml" counter missed covered
  [[ -f "$xml" ]] || { echo "n/a"; return; }
  # The last counter of each type in the file is the total for the whole project.
  counter="$(grep -o "<counter type=\"$1\"[^>]*>" "$xml" | tail -n 1)"
  missed="$(sed -E 's/.*missed="([0-9]+)".*/\1/' <<<"$counter")"
  covered="$(sed -E 's/.*covered="([0-9]+)".*/\1/' <<<"$counter")"
  awk -v c="$covered" -v m="$missed" \
    'BEGIN { printf "%d/%d (%.1f%%)", c, c + m, (c + m) ? 100 * c / (c + m) : 0 }'
}

# Prints the number of tests, failures, and errors across JUnit XML results.
junit_totals() {
  awk '
    /<testsuite / {
      for (i = 1; i <= NF; i++) {
        if ($i ~ /^tests=/)    { gsub(/[^0-9]/, "", $i); t += $i }
        if ($i ~ /^failures=/) { gsub(/[^0-9]/, "", $i); f += $i }
        if ($i ~ /^errors=/)   { gsub(/[^0-9]/, "", $i); e += $i }
      }
    }
    END { printf "%d tests, %d failures, %d errors", t, f, e }
  ' "$@" 2>/dev/null
}

{
  echo "Ticketing service test summary"
  echo "Generated: $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  echo "Commit:    $(git rev-parse --short HEAD 2>/dev/null || echo unknown)"
  echo
  echo "Gradle checks (unit + REST Assured API tests, coverage, Checkstyle, SpotBugs): $gradle_status"
  echo "  JUnit results:   $(junit_totals build/test-results/test/*.xml)"
  echo "  Line coverage:   $(coverage LINE)"
  echo "  Branch coverage: $(coverage BRANCH)"
  echo "Postman collection (live service + PostgreSQL): $live_status"
  if [[ -f "$REPORTS_DIR/postman/newman-results.xml" && "$live_status" != SKIPPED ]]; then
    echo "  Newman results:  $(junit_totals "$REPORTS_DIR/postman/newman-results.xml")"
  fi
  echo
  echo "Reports:"
  echo "  reports/tests/index.html        JUnit and REST Assured results"
  echo "  reports/coverage/index.html     JaCoCo coverage"
  echo "  reports/checkstyle/main.html    Checkstyle (also test.html)"
  echo "  reports/spotbugs/main.html      SpotBugs (also test.html)"
  echo "  reports/postman/                Newman results"
} | tee "$REPORTS_DIR/summary.txt"

if [[ "$gradle_status" == PASSED && "$live_status" != FAILED ]]; then
  log "All checks passed."
  exit 0
fi
log "Some checks failed. See the summary above."
exit 1
