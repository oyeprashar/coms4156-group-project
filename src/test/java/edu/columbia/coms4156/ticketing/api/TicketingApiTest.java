package edu.columbia.coms4156.ticketing.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import edu.columbia.coms4156.ticketing.entity.InventoryEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationStatus;
import edu.columbia.coms4156.ticketing.entity.VenueEntity;
import edu.columbia.coms4156.ticketing.repository.EventRepository;
import edu.columbia.coms4156.ticketing.repository.InventoryRepository;
import edu.columbia.coms4156.ticketing.repository.ReservationRepository;
import edu.columbia.coms4156.ticketing.repository.TicketRepository;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * API tests that send real HTTP requests to the running service and check the full
 * reservation lifecycle against the in-memory test database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TicketingApiTest {

  private static final String EVENT_ID = "evt_api";
  private static final int AVAILABLE_TICKETS = 5;

  @LocalServerPort private int port;
  private RequestSpecification api;

  @Autowired private EventRepository eventRepository;
  @Autowired private InventoryRepository inventoryRepository;
  @Autowired private ReservationRepository reservationRepository;
  @Autowired private TicketRepository ticketRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    api = new RequestSpecBuilder().setPort(port).build();

    transactionTemplate.executeWithoutResult(
        status -> {
          ticketRepository.deleteAll();
          reservationRepository.deleteAll();
          inventoryRepository.deleteAll();
          eventRepository.deleteAll();
          entityManager.flush();
          entityManager.createQuery("delete from VenueEntity").executeUpdate();

          VenueEntity venue = new VenueEntity("venue_api", "United Center", "Chicago");
          entityManager.persist(venue);

          OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
          EventEntity event =
              new EventEntity(
                  EVENT_ID,
                  "Chicago Bulls Night",
                  "sports",
                  now.plusDays(30),
                  new BigDecimal("75.00"),
                  new BigDecimal("220.00"),
                  "USD",
                  venue);
          entityManager.persist(event);
          entityManager.persist(new InventoryEntity(event, 10, AVAILABLE_TICKETS));

          entityManager.persist(
              new ReservationEntity(
                  "res_expired", event, "cust_api", 2, ReservationStatus.HELD,
                  now.minusMinutes(1)));
        });
  }

  /** Reserves tickets and returns the new reservation ID. */
  private String reserve(int quantity) {
    return given(api)
        .contentType(ContentType.JSON)
        .body(
            """
            {"eventId": "%s", "quantity": %d, "customerId": "cust_api"}
            """
                .formatted(EVENT_ID, quantity))
        .when()
        .post("/v1/reservations")
        .then()
        .statusCode(201)
        .body("status", equalTo("HELD"))
        .body("remainingTickets", equalTo(AVAILABLE_TICKETS - quantity))
        .extract()
        .path("reservationId");
  }

  @Test
  void searchFindsEventWithAvailability() {
    given(api)
        .queryParam("city", "chicago")
        .queryParam("category", "sports")
        .when()
        .get("/v1/events")
        .then()
        .statusCode(200)
        .body("count", equalTo(1))
        .body("events[0].eventId", equalTo(EVENT_ID))
        .body("events[0].venue.city", equalTo("Chicago"))
        .body("events[0].ticketAvailability.availableCount", equalTo(AVAILABLE_TICKETS));
  }

  @Test
  void searchWithInvalidPriceRangeIsRejected() {
    given(api)
        .queryParam("minPrice", 300)
        .queryParam("maxPrice", 100)
        .when()
        .get("/v1/events")
        .then()
        .statusCode(400)
        .body("error", notNullValue());
  }

  @Test
  void reserveThenConfirmIssuesOneTicketPerSeat() {
    String reservationId = reserve(2);

    given(api)
        .contentType(ContentType.JSON)
        .body(
            """
            {"customerId": "cust_api", "paymentReference": "PAY_1", "idempotencyKey": "k1"}
            """)
        .when()
        .post("/v1/reservations/{id}/confirm", reservationId)
        .then()
        .statusCode(200)
        .body("status", equalTo("CONFIRMED"))
        .body("tickets", hasSize(2));
  }

  @Test
  void reserveThenCancelReleasesTickets() {
    String reservationId = reserve(3);

    given(api)
        .contentType(ContentType.JSON)
        .body("{\"customerId\": \"cust_api\"}")
        .when()
        .post("/v1/reservations/{id}/cancel", reservationId)
        .then()
        .statusCode(200)
        .body("status", equalTo("CANCELLED"))
        .body("releasedTickets", equalTo(3))
        .body("availableTickets", equalTo(AVAILABLE_TICKETS));
  }

  @Test
  void overbookingIsRejected() {
    given(api)
        .contentType(ContentType.JSON)
        .body(
            """
            {"eventId": "%s", "quantity": %d, "customerId": "cust_api"}
            """
                .formatted(EVENT_ID, AVAILABLE_TICKETS + 1))
        .when()
        .post("/v1/reservations")
        .then()
        .statusCode(409)
        .body("error", equalTo("INSUFFICIENT_AVAILABILITY"))
        .body("availableQuantity", equalTo(AVAILABLE_TICKETS));
  }

  @Test
  void confirmingExpiredHoldIsRejected() {
    given(api)
        .contentType(ContentType.JSON)
        .body(
            """
            {"customerId": "cust_api", "paymentReference": "PAY_1", "idempotencyKey": "k1"}
            """)
        .when()
        .post("/v1/reservations/{id}/confirm", "res_expired")
        .then()
        .statusCode(409)
        .body("error", equalTo("RESERVATION_EXPIRED"));
  }

  @Test
  void reservingUnknownEventReturnsNotFound() {
    given(api)
        .contentType(ContentType.JSON)
        .body("{\"eventId\": \"evt_missing\", \"quantity\": 1, \"customerId\": \"cust_api\"}")
        .when()
        .post("/v1/reservations")
        .then()
        .statusCode(404);
  }
}
