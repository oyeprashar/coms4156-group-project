# coms4156-group-project

A minimal Spring Boot scaffold for a ticketing application inspired by BookMyShow
and District. A sample controller, service, and `Ticket` POJO demonstrate the
team's coding style. Business logic and database integration are future work.

The Gradle project and Spring application use the identifier `ticketing`.

## Basic setup

1. Install **Git** and **JDK 17 or JDK 26**. No separate Gradle installation or
   database is required. The first build needs internet access to download Gradle
   and dependencies.
2. Clone the repository and enter its root directory. Skip this step if you already
   have a local checkout.

   ```sh
   git clone https://github.com/oyeprashar/coms4156-group-project.git
   cd coms4156-group-project
   ```

3. Verify Java and the Gradle wrapper, then build the project. The build runs tests,
   generates coverage reports, checks Google Java Style, and creates executable JARs.

   ```sh
   java -version
   ./gradlew --version
   ./gradlew clean build
   ```

4. Start the application:

   ```sh
   ./gradlew bootRun
   ```

   Look for `Started TicketingApplication` in the console. The server listens at
   `http://localhost:8080`. No database is needed. There is no route at `/`, so
   that URL returns **404**. Stop the application with Ctrl+C.

5. Open `http://localhost:8080/v1/tickets/sample` in your browser, or run:

   ```sh
   curl http://localhost:8080/v1/tickets/sample
   ```

   The sample returns fixed data:

   ```json
   {
     "ticketId": "tkt-abc123",
     "eventId": "evt-abc123",
     "reservationId": "res-123abc"
   }
   ```

On Windows PowerShell, use `.\gradlew.bat` instead of `./gradlew` for each command.
If macOS/Linux reports `Permission denied`, run `chmod +x gradlew` and retry.
If port 8080 is occupied, use `./gradlew bootRun --args='--server.port=8081'`.

If Java is missing or Gradle reports the wrong JDK, set `JAVA_HOME` to your JDK's
installation directory (the directory containing `bin`), and put its `bin`
directory first on `PATH`. Replace the example paths below with your installed JDK.

macOS/Linux:

```sh
export JAVA_HOME="/path/to/jdk-17-or-26"
export PATH="$JAVA_HOME/bin:$PATH"
```

Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17-or-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

These commands apply to the current terminal session. Rerun `java -version` and
the wrapper's `--version` command after changing them.

### IDE setup

Open the repository root as a Gradle project. Select the included Gradle wrapper,
use JDK 17 or 26 as the Gradle JVM, and keep the Java language level at 17.
Delegate builds and tests to Gradle so Checkstyle, Mockito, and JaCoCo use the
project configuration. Enable EditorConfig support and select Google Java Style
in your Java formatter. The checked-in `.editorconfig` sets two-space Java
indentation and a 100-column line limit; Gradle Checkstyle is the style check used
by the build.

VS Code uses the shared [.vscode/settings.json](.vscode/settings.json) with the
Red Hat Java extension. It formats Java on save and organizes imports into static
imports first, then one alphabetically sorted group of ordinary imports, matching
Checkstyle. Java Auto Save runs when focus leaves the editor, so formatting and
import organization run together. The Java settings override a global formatter
such as Prettier for this project.

The local [Eclipse formatter profile](config/formatter/eclipse-java-google-style.xml)
is adapted from Google's
[last published profile](https://github.com/google/styleguide/blob/eaa0a05ec282e893ba314f1e6aeb9ef9f6674249/eclipse-java-google-style.xml).
It uses two-space indentation, 100-column wrapping, and the project's blank-line
rules. The profile is stored in the repository so formatting works offline and
does not change when an upstream URL changes. Eclipse users can import the same
profile. Checkstyle also checks naming and documentation rules that a formatter
cannot fix automatically.

After pulling these settings, run **Developer: Reload Window** in VS Code, then
**Format Document** and save any files formatted with the old settings. Verify
with `./gradlew checkstyleMain checkstyleTest`.

## Stack

| Tool | Version / purpose |
| --- | --- |
| Java | Java 17 baseline; build and run with JDK 17 or JDK 26 |
| Spring Boot | 4.1.1, Spring MVC with embedded Tomcat |
| Gradle | 9.8.1 via the included wrapper |
| JUnit | 6.0.3, managed by Spring Boot |
| Mockito | 5.23.0, managed by Spring Boot, with the Jupiter extension |
| JaCoCo | 0.8.15, HTML and XML coverage reports; the build fails below 80% line coverage |
| Checkstyle | 12.3.1, Google Java Style in `config/checkstyle/checkstyle.xml` |
| SpotBugs | 4.10.5 via Gradle plugin 6.5.13; exclusions in `config/spotbugs/exclude.xml` |
| REST Assured | 6.3.0, API tests that send real HTTP requests to the running service |
| Postman | Manual API testing with `postman/ticketing.postman_collection.json` |
| GitHub Actions | Runs all checks on every push and pull request |

Spring Boot manages test dependencies through `spring-boot-starter-webmvc-test`.
JUnit 6 still uses imports such as `org.junit.jupiter.api.Test`.

## Project structure

```text
.
├── build.gradle
├── settings.gradle
├── gradlew / gradlew.bat
├── gradle/wrapper/                       # Pinned Gradle distribution and checksum
├── compose.yaml                          # Local PostgreSQL container
├── .github/workflows/ci.yml              # GitHub Actions CI
├── config/checkstyle/checkstyle.xml      # Checkstyle rules (Google Java Style)
├── config/spotbugs/exclude.xml           # SpotBugs false-positive exclusions
├── postman/ticketing.postman_collection.json  # Postman/Newman API tests
├── scripts/run-all-tests.sh              # Master test script
├── reports/                              # Latest test, coverage, style, and SpotBugs reports
└── src/
    ├── main/
    │   ├── java/edu/columbia/coms4156/ticketing/
    │   │   ├── TicketingApplication.java # Spring Boot entry point
    │   │   ├── controller/               # HTTP routes and error responses
    │   │   ├── service/                  # Business logic and domain errors
    │   │   ├── entity/                   # JPA database entities
    │   │   ├── repository/               # Spring Data JPA repositories
    │   │   └── pojo/                     # Request and response records
    │   └── resources/
    │       ├── application.properties    # PostgreSQL connection settings
    │       └── db/seed.sql               # Sample venues, events, and inventory
    └── test/
        ├── java/edu/columbia/coms4156/ticketing/
        │   ├── service/                  # Unit tests with Mockito mocks
        │   ├── controller/               # MockMvc integration tests
        │   └── api/                      # REST Assured API tests
        └── resources/application.properties  # In-memory H2 database for tests
```

Packages contain `package-info.java` files documenting their purpose. Put new classes beneath the
`edu.columbia.coms4156.ticketing` package so Spring Boot can discover them.
Place future tests under the corresponding package in `src/test/java`.

### Sample coding style

Use `TicketController`, `TicketService`, and `Ticket` as small reference examples:

- Controllers define routes and delegate to services using constructor injection
  and `private final` dependency fields.
- Services contain application logic. The sample only returns a fixed ticket.
- POJOs hold data. `Ticket` uses a Java 17 record with three fields and no behavior.
- Keep descriptive names, public API Javadoc, and Google Java formatting. Run
  `./gradlew build` to check the same style rules for every new Java file.

## Java compatibility and packaging

The build uses the JDK selected by `JAVA_HOME` (or the Java on your `PATH` if unset).
Compilation uses `--release 17` to enforce Java 17 language features, standard
library APIs, and bytecode even when compiling on JDK 26. No Java 17 toolchain
download is required for developers using JDK 26. Tests run on the selected JDK.

To check both versions locally, run the build once with each JDK:

```sh
JAVA_HOME=/path/to/jdk-17 ./gradlew --no-daemon clean build
JAVA_HOME=/path/to/jdk-26 ./gradlew --no-daemon clean build
```

Build an executable JAR with `./gradlew bootJar`, then run it with:

```sh
java -jar build/libs/ticketing-0.0.1-SNAPSHOT.jar
```

## Tests, coverage, and style

### Run everything (master test script)

```sh
bash scripts/run-all-tests.sh             # all stages
bash scripts/run-all-tests.sh --skip-live # stage 1 only; no Docker or Node.js needed
```

[scripts/run-all-tests.sh](scripts/run-all-tests.sh) runs every check in order:

1. `./gradlew clean check`: JUnit/Mockito unit tests, REST Assured API tests, the
   JaCoCo coverage minimum, Checkstyle, and SpotBugs, using the in-memory H2 database.
2. Live API tests: starts PostgreSQL with Docker Compose, starts the service, loads
   `seed.sql`, and runs the Postman collection with Newman (fetched with `npx`).
   Needs Docker running and Node.js installed. The script stops the service, and
   the PostgreSQL container if it started it, when it finishes.
3. Copies all reports into [reports/](reports/) and writes
   [reports/summary.txt](reports/summary.txt) with test counts, line and branch
   coverage, and the result of each stage.

The script exits with a non-zero status if any stage fails. The `reports/` folder
is committed so the latest style, static analysis, test, and coverage reports are
in the repository. Rerun the script and commit `reports/` before tagging a release.
On Windows, run it from Git Bash or WSL.

### Individual commands

| Command | Purpose |
| --- | --- |
| `./gradlew test` | Run JUnit 6 and REST Assured tests and generate JaCoCo reports |
| `./gradlew jacocoTestReport` | Run tests if needed and generate coverage reports |
| `./gradlew jacocoTestCoverageVerification` | Fail if line coverage is below 80% |
| `./gradlew checkstyleMain checkstyleTest` | Check production and test Java style |
| `./gradlew spotbugsMain spotbugsTest` | Run SpotBugs static analysis on production and test code |
| `./gradlew check` | Run all of the above |
| `./gradlew build` | Run checks and create JARs |

Generated reports:

- Tests: `build/reports/tests/test/index.html`
- Coverage: `build/reports/jacoco/test/html/index.html`
- Coverage XML: `build/reports/jacoco/test/jacocoTestReport.xml`
- Style: `build/reports/checkstyle/main.html` and `test.html`
- Static analysis: `build/reports/spotbugs/main.html` and `test.html`

The master test script copies these into the committed [reports/](reports/) folder.
[reports/summary.txt](reports/summary.txt) shows the latest line and branch coverage
and test counts, and [reports/coverage/index.html](reports/coverage/index.html) has
the per-class JaCoCo branch coverage.

The [GitHub Actions workflow](.github/workflows/ci.yml) runs on pushes and pull
requests, and can also be started manually from the Actions tab. It checks Java
17 and 26 using `./gradlew --no-daemon --console=plain --continue check`. This runs
JUnit and REST Assured tests, the JaCoCo coverage minimum, Checkstyle, and SpotBugs
for production and test sources. `--continue` lets independent checks finish even
if another fails; any failure still fails the job. Each run uploads `build/reports/`
as a `reports-java-17` or `reports-java-26` artifact, downloadable from the run's
summary page in the Actions tab.

### Unit tests (JUnit 6 + Mockito)

Unit tests live in `src/test/java/edu/columbia/coms4156/ticketing/service/`, one
class per service operation (`EventServiceTest`, `ReservationServiceTest`,
`ReservationCancelServiceTest`). Each class uses `@ExtendWith(MockitoExtension.class)`
and replaces the repositories with `@Mock` test doubles, so the business logic runs
without a database. Shared setup is in each class's `@BeforeEach` method. Mockito is
loaded as a Java agent at test startup (configured in `build.gradle`) so it does not
need to attach itself dynamically on newer JDKs.

`ReservationControllerIntegrationTest` uses MockMvc and the in-memory H2 database
to check the confirm endpoint through Spring. `TicketingApplicationTests` checks
that the application starts.

### Configuration files

| File | Configures |
| --- | --- |
| `build.gradle` | Dependencies and versions, JUnit platform, Mockito agent, JaCoCo minimum, Checkstyle, SpotBugs |
| `src/test/resources/application.properties` | In-memory H2 database used by all Gradle tests |
| `src/main/resources/application.properties` | PostgreSQL connection for running the service |
| `config/checkstyle/checkstyle.xml` | Checkstyle rules (Google Java Style) |
| `config/spotbugs/exclude.xml` | SpotBugs exclusions, each with a reason |
| `postman/ticketing.postman_collection.json` | Postman requests, assertions, and variables |
| `compose.yaml` | PostgreSQL container used by `bootRun` and the master test script |
| `.github/workflows/ci.yml` | CI jobs on Java 17 and 26 |

### API tests (REST Assured)

`src/test/java/edu/columbia/coms4156/ticketing/api/TicketingApiTest.java` starts the
service on a random port with the in-memory H2 database and sends real HTTP
requests. It covers event search, reserve then confirm, reserve then cancel,
overbooking, confirming an expired hold, and unknown events. Add new end-to-end
scenarios there, using `given(api)` so each request targets the test server.

### Static analysis (SpotBugs)

SpotBugs runs at maximum effort and fails the build on any finding. If a finding
is a false positive, add a `<Match>` to `config/spotbugs/exclude.xml` with a comment
explaining why, rather than disabling the check. The current exclusions cover
exposed-reference warnings on JPA entities and response records, and newlines in
test JSON format strings.

### Manual testing (Postman)

Import `postman/ticketing.postman_collection.json` into Postman. Start PostgreSQL,
load `seed.sql`, and run `./gradlew bootRun` as described in Operation 1 below.
Then use **Run collection** to send the requests in order. "Reserve tickets" saves
the new reservation ID, which the confirm and cancel requests reuse. Change the
`baseUrl`, `eventId`, or `customerId` collection variables to target a different
server or seed record. The master test script runs the same collection
automatically with Newman.

### Google Java Style

Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
The configuration is vendored from
[Checkstyle 12.3.1's Google ruleset](https://github.com/checkstyle/checkstyle/blob/checkstyle-12.3.1/src/main/resources/google_checks.xml)
to match the version in `build.gradle`. Keep these versions aligned when upgrading.
The rules include:

- Two-space block indentation and at least four extra spaces for continuation lines.
- A 100-column line limit, with the Google ruleset's exceptions for imports,
  package declarations, and other supported cases.
- Explicit imports, with static imports first and alphabetically sorted groups.
- Brace placement, naming, whitespace, and Javadoc checks.

Run `./gradlew checkstyleMain checkstyleTest` before submitting changes, or run
`./gradlew build` to include all checks. The upstream rules report warnings;
`maxWarnings = 0` in `build.gradle` makes those violations fail the build.
Open `build/reports/checkstyle/main.html` or `test.html` to see the affected files
and lines. Checkstyle reports violations; use your editor to fix or format the code.
The guide remains the reference for conventions Checkstyle cannot verify.

The build requires at least 80% line coverage across all classes. Add tests with new
code so the minimum keeps passing; the HTML coverage report shows which lines are
untested.

## Database recommendation

**PostgreSQL is the recommended starting point.** Events, venues, showtimes, seats,
users, and bookings have natural relationships. PostgreSQL's foreign keys help
keep those relationships valid, and transactions let related booking changes
succeed or fail together. For reserved seating, a unique constraint on the active
allocation of a `(showtime_id, seat_id)` pair, combined with transactional booking
logic, can prevent two customers from booking the same seat. A read of seat
availability alone is not enough to prevent concurrent bookings. See PostgreSQL's
[constraints documentation](https://www.postgresql.org/docs/current/ddl-constraints.html)
and [transaction tutorial](https://www.postgresql.org/docs/current/tutorial-transactions.html).

Start with a single PostgreSQL database when persistence is implemented. At that
point, add Spring Data JPA, the PostgreSQL JDBC driver, and schema migrations.
This scaffold has no database dependency, connection credentials, or schema,
so setup does not depend on choosing or running a database.

## Compatibility references

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/4.1/system-requirements.html)
- [Spring Boot managed dependencies](https://docs.spring.io/spring-boot/4.1/appendix/dependency-versions/coordinates.html)
- [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- [Checkstyle JRE requirements](https://checkstyle.org/#JRE_and_JDK)

## Team: DROP TABLE TEAMS;

- Aditya Unnikrishnan - au2325
- Shubham Prashar - sp4540
- Tyra Minnal Vinay - tl3441
- Jun Hong - jyh2138
- Madhav Rajkondawar - mr4650
- Amelie Dequito - amd2398


## Operation 1: Search Events API

### Overview

The Event Search API allows clients to search upcoming events using optional filters such as city, category, date range, ticket price, and event name.

Events are retrieved from PostgreSQL using Spring Data JPA. Each response includes event information, venue details, pricing, and current ticket availability.

### Endpoint

```http
GET /v1/events
```

**Base URL:** `http://localhost:8080`

### Query Parameters

All parameters are optional.

| Parameter | Type | Description | Default |
|---|---|---|---|
| `city` | String | Case-insensitive exact city match | All cities |
| `category` | String | Case-insensitive exact category match | All categories |
| `startDate` | Date (`YYYY-MM-DD`) | Events on or after this UTC date | No additional lower bound |
| `endDate` | Date (`YYYY-MM-DD`) | Events on or before this UTC date | No upper bound |
| `minPrice` | Decimal | Minimum desired ticket price | No minimum |
| `maxPrice` | Decimal | Maximum desired ticket price | No maximum |
| `keyword` | String | Case-insensitive partial match against event names | All names |
| `page` | Integer | Zero-indexed page number | `0` |
| `pageSize` | Integer | Number of events per page (1–100) | `10` |

All supplied filters are combined using AND conditions.

Price filtering uses range overlap: an event matches when its advertised minimum-to-maximum price range overlaps the requested price range.

Only upcoming events are returned, sorted by start time and then event ID.

### Running the Service

**Prerequisites:**
- Java 17
- Docker and Docker Compose
- PostgreSQL container configuration in `compose.yaml`

**1. Start PostgreSQL**

From the project root:

```bash
docker compose up -d postgres
```

**2. Populate the database**

The database tables are generated from the JPA entities when the Spring Boot application starts, using `spring.jpa.hibernate.ddl-auto=update`.

For a fresh database, start the application once to create the tables, then load the sample data:

```bash
docker compose exec -T postgres \
  psql -U ticketing -d ticketing \
  < src/main/resources/db/seed.sql
```

The seed script includes sample venues, events, and ticket inventory. It can be rerun without duplicating existing records.

**3. Run the Spring Boot application**

```bash
./gradlew bootRun
```

The API is accessible at `http://localhost:8080`.

The local PostgreSQL container is exposed on port `5433`, as configured in `compose.yaml`.

### Example Requests

**1. Retrieve upcoming events**

```bash
curl "http://localhost:8080/v1/events"
```

Returns the first 10 upcoming events.

**2. Filter by city**

```bash
curl "http://localhost:8080/v1/events?city=Chicago"
```

Returns upcoming events hosted in Chicago.

**3. Combine city and category**

```bash
curl "http://localhost:8080/v1/events?city=New%20York&category=concert"
```

Returns upcoming concerts in New York.

**4. Search by keyword**

```bash
curl "http://localhost:8080/v1/events?keyword=Coldplay"
```

Returns upcoming events whose names contain `Coldplay`.

**5. Search by date range**

```bash
curl "http://localhost:8080/v1/events?startDate=2026-11-01&endDate=2026-12-05"
```

Returns events occurring within the specified UTC date range.

**6. Search by ticket price**

```bash
curl "http://localhost:8080/v1/events?minPrice=100&maxPrice=150"
```

Returns events whose ticket price ranges overlap $100–$150.

**7. Pagination**

```bash
curl "http://localhost:8080/v1/events?page=1&pageSize=3"
```

Returns the second page, containing up to three events.

### Example Successful Response

**HTTP 200 OK**

```json
{
  "events": [
    {
      "eventId": "evt_001",
      "name": "Coldplay Live",
      "category": "concert",
      "venue": {
        "venueId": "venue_001",
        "name": "Madison Square Garden",
        "city": "New York"
      },
      "startTime": "2026-11-16T00:30:00Z",
      "ticketAvailability": {
        "available": true,
        "availableCount": 842
      },
      "priceRange": {
        "min": 85.00,
        "max": 240.00,
        "currency": "USD"
      }
    }
  ],
  "count": 1
}
```

The `events` array contains the current page of results. The `count` field represents the total number of matching events across all pages.

### Error Handling

Invalid search parameters return HTTP 400.

| Scenario | Example | Response |
|---|---|---|
| Invalid price range | `minPrice=300&maxPrice=100` | `400 Bad Request` |
| Negative price | `minPrice=-10` | `400 Bad Request` |
| Invalid date range | `startDate=2027-05-01&endDate=2026-11-01` | `400 Bad Request` |
| Negative page | `page=-1` | `400 Bad Request` |
| Invalid page size | `pageSize=0` or `pageSize=101` | `400 Bad Request` |
| Invalid parameter format | `page=abc` | `400 Bad Request` |

**Example invalid request:**

```bash
curl -i "http://localhost:8080/v1/events?minPrice=300&maxPrice=100"
```

**Response:**

```json
{
  "error": "minPrice cannot exceed maxPrice"
}
```

Invalid parameter types are rejected by Spring's request parameter binding; their error response body may differ from the example above.

### No Matching Events

A valid search that produces no matching events returns HTTP 200 with an empty array.

```bash
curl "http://localhost:8080/v1/events?city=Los%20Angeles"
```

**Response:**

```json
{
  "events": [],
  "count": 0
}
```

### Data and Side Effects

- **Database:** PostgreSQL
- **Database tables:** `events`, `venues`, `inventory`
- **Persistence:** Spring Data JPA
- **Read-only:** This endpoint does not create, update, reserve, or delete tickets or events.
- **Availability:** Retrieved from the inventory table at


## Operation 3: Confirm Reservation API

### Overview

The Confirm Reservation API turns a held reservation into issued tickets. It checks that
the reservation belongs to the caller and is still within its hold window, then marks it
`CONFIRMED` and creates one ticket per reserved seat in the same transaction.

Payment is out of scope: the service stores the client's payment reference but does not
verify it.

### Endpoint

```http
POST /v1/reservations/{reservationId}/confirm
```

### Request Body

| Field | Type | Description |
|---|---|---|
| `customerId` | String | Customer that owns the reservation |
| `paymentReference` | String | Client-supplied payment reference, stored as-is |
| `idempotencyKey` | String | Key identifying this confirmation attempt for safe retries |

All three fields are required.

### Behavior

- **Ownership:** the reservation's `customerId` must match the request.
- **Hold window:** a `HELD` reservation can be confirmed only before its `expiresAt` time.
  If the hold has expired, the reservation is marked `EXPIRED`, its tickets are returned
  to the event's available inventory, and the request is rejected. The expiry is saved
  even though the request fails, so the tickets are released exactly once.
- **Safe retries:** repeating a successful confirmation with the same `idempotencyKey`
  returns the original tickets and confirmation time without issuing new tickets. A
  different key on an already confirmed reservation is rejected.
- **Concurrency:** the reservation row is locked (`SELECT ... FOR UPDATE`) for the whole
  confirmation, so simultaneous requests are applied one at a time.
- **Inventory:** confirming does not change inventory, because tickets were already held
  when the reservation was created.

### Example Request

```bash
curl -X POST "http://localhost:8080/v1/reservations/res_demo_001/confirm" \
  -H "Content-Type: application/json" \
  -d '{"customerId": "cust_001", "paymentReference": "PAY_123", "idempotencyKey": "demo-key-1"}'
```

`res_demo_001` is a sample `HELD` reservation in `seed.sql` for trying this endpoint
before the reservation API (Operation 2) is available.

### Example Successful Response

**HTTP 200 OK**

```json
{
  "reservationId": "res_demo_001",
  "eventId": "evt_001",
  "status": "CONFIRMED",
  "tickets": [
    { "ticketId": "tkt_0c9e5b1e-6d0a-4b3f-9f53-1f7a2c4e8d11" },
    { "ticketId": "tkt_7a41d2c8-3e5f-4c6b-8d9a-2b1e0f3c5a77" }
  ],
  "confirmedAt": "2026-10-25T16:55:02.123Z"
}
```

### Error Responses

| HTTP status | `error` | When |
|---|---|---|
| 400 | Message naming the required fields | A request field is missing or blank |
| 403 | `RESERVATION_NOT_OWNED` | `customerId` does not match the reservation |
| 404 | `RESERVATION_NOT_FOUND` | No reservation has this ID |
| 409 | `RESERVATION_EXPIRED` | The hold window has passed |
| 409 | `RESERVATION_CANCELLED` | The reservation was cancelled |
| 409 | `ALREADY_CONFIRMED` | Already confirmed with a different idempotency key |

Error bodies include the reservation ID, for example:

```json
{
  "error": "RESERVATION_EXPIRED",
  "reservationId": "res_789"
}
```

### Data and Side Effects

- **Database tables:** `reservations` (reads and updates status), `tickets` (inserts),
  `inventory` (adds tickets back only when an expired hold is found)
- **Ticket states:** issued tickets start as `ISSUED`; ticket verification (Operation 4)
  reads them from the `tickets` table.
- **Reservation states:** `HELD`, `CONFIRMED`, `EXPIRED`, `CANCELLED`. The reservation
  table is shared with Operations 2 and 5.

## Operation 5: Cancel Reservation API

### Overview

The Cancel Reservation API allows clients to release a held reservation before it is
confirmed. It verifies that the reservation belongs to the caller and is currently in
the `HELD` state, updates its status to `CANCELLED`, and returns the reserved tickets back
to the event's available inventory in a single transaction.

Once cancelled, tickets are immediately available for other customers to reserve.

### Endpoint

```http
POST /v1/reservations/{reservationId}/cancel
```

### Request Body

| Field | Type | Description |
|---|---|---|
| `customerId` | String | Customer that owns the reservation |

`customerId` is required.

### Behavior

- **Ownership:** The reservation's `customerId` must match the request. If another
  customer attempts to cancel the reservation, the request is rejected with `403 Forbidden`.
- **Hold state:** Only `HELD` reservations can be cancelled. If a reservation is already
  confirmed, it cannot be cancelled and is rejected with `409 Conflict`.
- **Inventory restoration:** When successfully cancelled, the reservation's quantity of
  tickets is atomically added back to the event's available inventory.
- **Concurrency:** The reservation row is locked (`SELECT ... FOR UPDATE`) during the
  transaction to prevent race conditions from releasing tickets more than once.

### Example Request

```bash
curl -X POST "http://localhost:8080/v1/reservations/res_demo_001/cancel" \
  -H "Content-Type: application/json" \
  -d '{"customerId": "cust_001"}'
```

### Example Successful Response

**HTTP 200 OK**

```json
{
  "reservationId": "res_demo_001",
  "eventId": "evt_001",
  "status": "CANCELLED",
  "releasedTickets": 2,
  "availableTickets": 844
}
```

### Error Responses

| HTTP status | `error` | When |
|---|---|---|
| 400 | `customerId is required` | `customerId` is missing or blank |
| 403 | `RESERVATION_NOT_OWNED` | `customerId` does not match the reservation |
| 404 | `RESERVATION_NOT_FOUND` | No reservation exists with this ID |
| 409 | `RESERVATION_ALREADY_CONFIRMED` | The reservation has already been confirmed |

Error bodies for domain errors include the reservation ID:

```json
{
  "error": "RESERVATION_NOT_OWNED",
  "reservationId": "res_demo_001"
}
```

### Data and Side Effects

- **Database tables:** `reservations` (updates status to `CANCELLED`), `inventory` (increases `available_tickets` by the released quantity)
- **Reservation states:** Transitions from `HELD` to `CANCELLED`.
- **Inventory:** Returns tickets to inventory using an atomic update so availability counts stay consistent under concurrent traffic.


## AI Usage
This project uses AI tools to assist with code generation, documentation, and testing. The team has reviewed and validated all AI-generated content to ensure accuracy, security, and compliance with project requirements.

### Tools Used
- **Aditya Unnikrishnan:** ChatGPT Codex for code generation and documentation assistance.
   Model used: GPT-6 Astra \
   01a13cf6-ea07-77a3-8cf0-395c9d26c937 (Project setup)
   Context: 31% left (178,327 used / 258K)

   01a17933-fd53-7cf1-a373-fd493e531d29 (Test compliance and documentation coverage)
   Context: 38% left (159,537 used / 258K)

   Codex acquired through free student offer from OpenAI.

- **Jun Hong:** Claude Sonnet 5.5 for code generation and documentation assistance.

  Cumulative Tokens: ~700k (every back and forth, resending context)

  Unique Content Tokens: ~105k (counting all messages just once without re-reading context)

  Used Claude to generate an outline of Operation 5, and then I reviewed and revised it.
  After I started coding following the outline, I asked Claude for assistance if I was stuck
  and also to double check my work. Claude generated the unit tests after I reviewed their plan.
  Lastly, Claude also generated my part of the README after I input all the necessary information.

- **Madhav Rajkondawar:** Claude Code for testing/tooling, setup, and the master test script.
   Model used: Claude Opus 5.5 \
   4bbebf75-926e-4ce0-a5bb-dc0f22b7fbe6 (Testing tools, API tests, and master test script)
   Context: 85% left (154,500 used / 1M)

   I leveraged Claude to add the general tools from the proposal as well as the associated files
   needed to take care of the testing. I also added the test script which checks all the flows and
   API testing then added to the README document for all my changes. I used Claude's assistance on
   these and reviewed the changes after they were made as well as added to them where needed.
   Claude Pro Subscription has been utilized to achieve this.

- **Shubham Prashar:** ChatGPT and Cursor for implementation planning, code review, unit testing, and documentation assistance.

  Cumulative Tokens: ~450k

  Unique Content Tokens: ~70k

  Used AI to plan and break down Operation 1 (Event Search) into smaller implementation tasks.
  I reviewed the suggested approach before implementing the controller and service logic.
  I also used AI to troubleshoot issues, review code quality, and identify potential edge cases.
  Finally, I used AI to assist with unit tests, Checkstyle fixes, and documentation, reviewing the
  generated output for correctness.
