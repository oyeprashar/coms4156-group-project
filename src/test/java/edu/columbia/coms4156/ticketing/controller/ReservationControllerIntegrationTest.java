package edu.columbia.coms4156.ticketing.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import edu.columbia.coms4156.ticketing.entity.InventoryEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationStatus;
import edu.columbia.coms4156.ticketing.entity.VenueEntity;
import edu.columbia.coms4156.ticketing.repository.EventRepository;
import edu.columbia.coms4156.ticketing.repository.InventoryRepository;
import edu.columbia.coms4156.ticketing.repository.ReservationRepository;
import edu.columbia.coms4156.ticketing.repository.TicketRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/** End-to-end tests for confirming reservations against the in-memory test database. */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EventRepository eventRepository;
  @Autowired private InventoryRepository inventoryRepository;
  @Autowired private ReservationRepository reservationRepository;
  @Autowired private TicketRepository ticketRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    transactionTemplate.executeWithoutResult(
        status -> {
          ticketRepository.deleteAll();
          reservationRepository.deleteAll();
          inventoryRepository.deleteAll();
          eventRepository.deleteAll();
          entityManager.flush();
          entityManager.createQuery("delete from VenueEntity").executeUpdate();

          VenueEntity venue = new VenueEntity("venue_001", "Madison Square Garden", "New York");
          entityManager.persist(venue);

          EventEntity event =
              new EventEntity(
                  "evt_001",
                  "Coldplay Live",
                  "concert",
                  OffsetDateTime.now(ZoneOffset.UTC).plusDays(30),
                  new BigDecimal("85.00"),
                  new BigDecimal("240.00"),
                  "USD",
                  venue);
          entityManager.persist(event);
          entityManager.persist(new InventoryEntity(event, 100, 40));

          OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
          entityManager.persist(
              new ReservationEntity(
                  "res_held", event, "cust_001", 2, ReservationStatus.HELD, now.plusMinutes(10)));
          entityManager.persist(
              new ReservationEntity(
                  "res_stale", event, "cust_001", 3, ReservationStatus.HELD, now.minusMinutes(1)));
        });
  }

  private ResultActions confirm(String reservationId, String customerId, String key)
      throws Exception {

    String body =
        """
        {"customerId": "%s", "paymentReference": "PAY_123", "idempotencyKey": "%s"}
        """
            .formatted(customerId, key);

    return mockMvc.perform(
        post("/v1/reservations/{id}/confirm", reservationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void confirmHeldReservationIssuesTickets() throws Exception {
    confirm("res_held", "cust_001", "key-1")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservationId").value("res_held"))
        .andExpect(jsonPath("$.eventId").value("evt_001"))
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.tickets.length()").value(2))
        .andExpect(jsonPath("$.confirmedAt").exists());

    assertEquals(2, ticketRepository.findByReservationReservationIdOrderByTicketId("res_held").size());
    assertEquals(
        ReservationStatus.CONFIRMED,
        reservationRepository.findById("res_held").orElseThrow().getStatus());
    assertEquals(40, inventoryRepository.findById("evt_001").orElseThrow().getAvailableTickets());
  }

  @Test
  void retryWithSameKeyDoesNotIssueMoreTickets() throws Exception {
    String first =
        confirm("res_held", "cust_001", "key-1").andReturn().getResponse().getContentAsString();
    String second =
        confirm("res_held", "cust_001", "key-1")
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertEquals(first, second);
    assertEquals(2, ticketRepository.findByReservationReservationIdOrderByTicketId("res_held").size());
  }

  @Test
  void confirmWithNewKeyAfterConfirmationReturnsConflict() throws Exception {
    confirm("res_held", "cust_001", "key-1").andExpect(status().isOk());

    confirm("res_held", "cust_001", "key-2")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("ALREADY_CONFIRMED"))
        .andExpect(jsonPath("$.reservationId").value("res_held"));
  }

  @Test
  void confirmExpiredHoldCommitsExpiryAndReleasesTickets() throws Exception {
    confirm("res_stale", "cust_001", "key-1")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("RESERVATION_EXPIRED"));

    assertEquals(
        ReservationStatus.EXPIRED,
        reservationRepository.findById("res_stale").orElseThrow().getStatus());
    assertEquals(43, inventoryRepository.findById("evt_001").orElseThrow().getAvailableTickets());

    confirm("res_stale", "cust_001", "key-2").andExpect(status().isConflict());
    assertEquals(43, inventoryRepository.findById("evt_001").orElseThrow().getAvailableTickets());
  }

  @Test
  void confirmUnknownReservationReturnsNotFound() throws Exception {
    confirm("res_missing", "cust_001", "key-1")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("RESERVATION_NOT_FOUND"));
  }

  @Test
  void confirmByAnotherCustomerReturnsForbidden() throws Exception {
    confirm("res_held", "cust_999", "key-1")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error").value("RESERVATION_NOT_OWNED"));
  }

  @Test
  void confirmWithMissingFieldsReturnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/v1/reservations/{id}/confirm", "res_held")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\": \"cust_001\"}"))
        .andExpect(status().isBadRequest());
  }
}
