package edu.columbia.coms4156.ticketing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import edu.columbia.coms4156.ticketing.entity.InventoryEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationStatus;
import edu.columbia.coms4156.ticketing.entity.TicketEntity;
import edu.columbia.coms4156.ticketing.entity.TicketStatus;
import edu.columbia.coms4156.ticketing.entity.VenueEntity;
import edu.columbia.coms4156.ticketing.pojo.ConfirmReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.ConfirmReservationResponse;
import edu.columbia.coms4156.ticketing.pojo.IssuedTicket;
import edu.columbia.coms4156.ticketing.pojo.ReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.ReservationResponse;
import edu.columbia.coms4156.ticketing.repository.EventRepository;
import edu.columbia.coms4156.ticketing.repository.InventoryRepository;
import edu.columbia.coms4156.ticketing.repository.ReservationRepository;
import edu.columbia.coms4156.ticketing.repository.TicketRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Unit tests for ReservationService. */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

  private static final String RESERVATION_ID = "res_001";
  private static final String CUSTOMER_ID = "cust_001";

  @Mock private EventRepository eventRepository;
  @Mock private InventoryRepository inventoryRepository;
  @Mock private ReservationRepository reservationRepository;
  @Mock private TicketRepository ticketRepository;

  @Mock private EventEntity reservationEvent;
  @Mock private InventoryEntity inventory;

  private ReservationService reservationService;
  private EventEntity event;

  @BeforeEach
  void setUp() {
    reservationService =
      new ReservationService(
        eventRepository, inventoryRepository, reservationRepository, ticketRepository);

    VenueEntity venue = new VenueEntity("venue_001", "Madison Square Garden", "New York");
    event =
      new EventEntity(
        "evt_001",
        "Coldplay Live",
        "concert",
        OffsetDateTime.now(ZoneOffset.UTC).plusDays(30),
        new BigDecimal("85.00"),
        new BigDecimal("240.00"),
        "USD",
        venue);
  }

  /** Configures a valid event with available inventory. */
  private void setUpAvailableInventory(int availableTickets) {
    when(eventRepository.findById("evt_001")).thenReturn(Optional.of(reservationEvent));

    when(inventoryRepository.findByEventIdForUpdate("evt_001"))
      .thenReturn(Optional.of(inventory));

    when(inventory.getAvailableTickets()).thenReturn(availableTickets);
  }

  /** Verifies that a successful reservation holds tickets. */
  @Test
  void reserveTicketsSucceeds() {
    setUpAvailableInventory(100);

    ReservationRequest request = new ReservationRequest("evt_001", 2, "cust_456");

    ReservationResponse response = reservationService.reserveTickets(request);

    assertNotNull(response);
    assertEquals("evt_001", response.eventId());
    assertEquals(2, response.quantity());
    assertEquals("HELD", response.status());
    assertEquals(98, response.remainingTickets());
    assertTrue(response.reservationId().startsWith("res_"));

    verify(inventory).setAvailableTickets(98);
    verify(inventoryRepository).save(inventory);
    verify(reservationRepository).save(any(ReservationEntity.class));
  }

  /** Verifies that negative quantities are rejected. */
  @Test
  void reserveTicketsRejectsNegativeQuantity() {
    ReservationRequest request = new ReservationRequest("evt_001", -2, "cust_456");

    assertThrows(IllegalArgumentException.class, () -> reservationService.reserveTickets(request));

    verifyNoInteractions(eventRepository, inventoryRepository, reservationRepository);
  }

  /** Verifies that zero quantities are rejected. */
  @Test
  void reserveTicketsRejectsZeroQuantity() {
    ReservationRequest request = new ReservationRequest("evt_001", 0, "cust_456");

    assertThrows(IllegalArgumentException.class, () -> reservationService.reserveTickets(request));

    verifyNoInteractions(eventRepository, inventoryRepository, reservationRepository);
  }

  /** Verifies that null requests are rejected. */
  @Test
  void reserveTicketsRejectsNullRequest() {
    assertThrows(IllegalArgumentException.class, () -> reservationService.reserveTickets(null));

    verifyNoInteractions(eventRepository, inventoryRepository, reservationRepository);
  }

  /** Verifies that null event IDs are rejected. */
  @Test
  void reserveTicketsRejectsNullEventId() {
    ReservationRequest request = new ReservationRequest(null, 2, "cust_456");

    assertThrows(IllegalArgumentException.class, () -> reservationService.reserveTickets(request));
  }

  /** Verifies that blank event IDs are rejected. */
  @Test
  void reserveTicketsRejectsBlankEventId() {
    ReservationRequest request = new ReservationRequest(" ", 2, "cust_456");

    assertThrows(IllegalArgumentException.class, () -> reservationService.reserveTickets(request));
  }

  /** Verifies that null customer IDs are rejected. */
  @Test
  void reserveTicketsRejectsNullCustomerId() {
    ReservationRequest request = new ReservationRequest("evt_001", 2, null);

    assertThrows(IllegalArgumentException.class, () -> reservationService.reserveTickets(request));
  }

  /** Verifies that blank customer IDs are rejected. */
  @Test
  void reserveTicketsRejectsBlankCustomerId() {
    ReservationRequest request = new ReservationRequest("evt_001", 2, " ");

    assertThrows(IllegalArgumentException.class, () -> reservationService.reserveTickets(request));
  }

  /** Verifies that nonexistent events return a 404 exception. */
  @Test
  void reserveTicketsRejectsNonexistentEvent() {
    when(eventRepository.findById("evt_999")).thenReturn(Optional.empty());

    ReservationRequest request = new ReservationRequest("evt_999", 2, "cust_456");

    ResponseStatusException exception =
      assertThrows(
        ResponseStatusException.class, () -> reservationService.reserveTickets(request));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    verifyNoInteractions(inventoryRepository, reservationRepository);
  }

  /** Verifies that missing inventory is rejected. */
  @Test
  void reserveTicketsRejectsMissingInventory() {
    when(eventRepository.findById("evt_001")).thenReturn(Optional.of(reservationEvent));

    when(inventoryRepository.findByEventIdForUpdate("evt_001"))
      .thenReturn(Optional.empty());

    ReservationRequest request = new ReservationRequest("evt_001", 2, "cust_456");

    assertThrows(IllegalArgumentException.class, () -> reservationService.reserveTickets(request));

    verifyNoInteractions(reservationRepository);
  }

  /** Verifies that insufficient inventory is rejected. */
  @Test
  void reserveTicketsRejectsInsufficientInventory() {
    setUpAvailableInventory(1);

    ReservationRequest request = new ReservationRequest("evt_001", 2, "cust_456");

    assertThrows(
      InsufficientAvailabilityException.class, () -> reservationService.reserveTickets(request));

    verify(inventoryRepository, never()).save(any(InventoryEntity.class));
    verify(reservationRepository, never()).save(any(ReservationEntity.class));
  }

  /** Verifies that exactly available tickets can be reserved. */
  @Test
  void reserveTicketsAcceptsExactInventory() {
    setUpAvailableInventory(5);

    ReservationRequest request = new ReservationRequest("evt_001", 5, "cust_456");

    ReservationResponse response = reservationService.reserveTickets(request);

    assertEquals(0, response.remainingTickets());
    verify(inventory).setAvailableTickets(0);
    verify(inventoryRepository).save(inventory);
    verify(reservationRepository).save(any(ReservationEntity.class));
  }

  /** Verifies that the reservation is saved to the repository. */
  @Test
  void reserveTicketsSavesReservation() {
    setUpAvailableInventory(100);

    ReservationRequest request = new ReservationRequest("evt_001", 3, "cust_456");

    ReservationResponse response = reservationService.reserveTickets(request);

    ArgumentCaptor<ReservationEntity> captor = ArgumentCaptor.forClass(ReservationEntity.class);

    verify(reservationRepository).save(captor.capture());

    ReservationEntity savedReservation = captor.getValue();

    assertNotNull(savedReservation);
    assertEquals(response.reservationId(), savedReservation.getReservationId());
    assertEquals(3, savedReservation.getQuantity());
    assertEquals(ReservationStatus.HELD, savedReservation.getStatus());
  }

  /** Verifies that reservation holds expire approximately ten minutes later. */
  @Test
  void reserveTicketsSetsExpirationTime() {
    setUpAvailableInventory(100);

    OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

    ReservationResponse response =
      reservationService.reserveTickets(new ReservationRequest("evt_001", 2, "cust_456"));

    OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

    assertNotNull(response.expiresAt());
    assertTrue(response.expiresAt().isAfter(before.plusMinutes(9)));
    assertTrue(response.expiresAt().isBefore(after.plusMinutes(11)));
  }

  private ReservationEntity reservation(ReservationStatus status, OffsetDateTime expiresAt) {
    ReservationEntity reservation =
      new ReservationEntity(RESERVATION_ID, event, CUSTOMER_ID, 2, status, expiresAt);
    when(reservationRepository.findByIdForUpdate(RESERVATION_ID))
      .thenReturn(Optional.of(reservation));
    return reservation;
  }

  private ReservationEntity heldReservation() {
    return reservation(ReservationStatus.HELD, OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10));
  }

  private static ConfirmReservationRequest request(String idempotencyKey) {
    return new ConfirmReservationRequest(CUSTOMER_ID, "PAY_123", idempotencyKey);
  }

  @Test
  @SuppressWarnings("unchecked")
  void confirmHeldReservationIssuesOneTicketPerSeat() {
    when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    final ReservationEntity reservation = heldReservation();

    ConfirmReservationResponse response =
      reservationService.confirmReservation(RESERVATION_ID, request("key-1"));

    assertEquals(RESERVATION_ID, response.reservationId());
    assertEquals("evt_001", response.eventId());
    assertEquals("CONFIRMED", response.status());
    assertEquals(2, response.tickets().size());
    assertNotNull(response.confirmedAt());

    assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
    assertEquals("PAY_123", reservation.getPaymentReference());
    assertEquals("key-1", reservation.getConfirmIdempotencyKey());

    ArgumentCaptor<List<TicketEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(ticketRepository).saveAll(captor.capture());
    List<TicketEntity> saved = captor.getValue();
    assertEquals(2, saved.size());
    saved.forEach(
      ticket -> {
        assertEquals(TicketStatus.ISSUED, ticket.getStatus());
        assertEquals(reservation, ticket.getReservation());
        assertEquals(event, ticket.getEvent());
      });
    verify(inventoryRepository, never()).releaseTickets(anyString(), anyInt());
  }

  @Test
  void confirmIssuesDistinctTicketIds() {
    heldReservation();
    when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

    ConfirmReservationResponse response =
      reservationService.confirmReservation(RESERVATION_ID, request("key-1"));

    assertEquals(
      2, response.tickets().stream().map(IssuedTicket::ticketId).distinct().count());
  }

  @Test
  void confirmMissingReservationIsRejected() {
    when(reservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.empty());

    ReservationConfirmationException exception =
      assertThrows(
        ReservationConfirmationException.class,
        () -> reservationService.confirmReservation(RESERVATION_ID, request("key-1")));

    assertEquals(ConfirmationError.RESERVATION_NOT_FOUND, exception.getError());
    assertEquals(RESERVATION_ID, exception.getReservationId());
  }

  @Test
  void confirmByAnotherCustomerIsRejected() {
    ReservationEntity reservation = heldReservation();

    ReservationConfirmationException exception =
      assertThrows(
        ReservationConfirmationException.class,
        () ->
          reservationService.confirmReservation(
            RESERVATION_ID, new ConfirmReservationRequest("cust_999", "PAY_123", "k")));

    assertEquals(ConfirmationError.RESERVATION_NOT_OWNED, exception.getError());
    assertEquals(ReservationStatus.HELD, reservation.getStatus());
    verify(ticketRepository, never()).saveAll(anyList());
  }

  @Test
  void confirmExpiredHoldMarksExpiredAndReleasesInventory() {
    ReservationEntity reservation =
      reservation(ReservationStatus.HELD, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

    ReservationConfirmationException exception =
      assertThrows(
        ReservationConfirmationException.class,
        () -> reservationService.confirmReservation(RESERVATION_ID, request("key-1")));

    assertEquals(ConfirmationError.RESERVATION_EXPIRED, exception.getError());
    assertEquals(ReservationStatus.EXPIRED, reservation.getStatus());
    verify(reservationRepository).save(reservation);
    verify(inventoryRepository).releaseTickets("evt_001", 2);
    verify(ticketRepository, never()).saveAll(anyList());
  }

  @Test
  void confirmAlreadyExpiredReservationDoesNotReleaseAgain() {
    reservation(ReservationStatus.EXPIRED, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

    ReservationConfirmationException exception =
      assertThrows(
        ReservationConfirmationException.class,
        () -> reservationService.confirmReservation(RESERVATION_ID, request("key-1")));

    assertEquals(ConfirmationError.RESERVATION_EXPIRED, exception.getError());
    verify(inventoryRepository, never()).releaseTickets(anyString(), anyInt());
  }

  @Test
  void confirmCancelledReservationIsRejected() {
    reservation(ReservationStatus.CANCELLED, OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10));

    ReservationConfirmationException exception =
      assertThrows(
        ReservationConfirmationException.class,
        () -> reservationService.confirmReservation(RESERVATION_ID, request("key-1")));

    assertEquals(ConfirmationError.RESERVATION_CANCELLED, exception.getError());
    verify(inventoryRepository, never()).releaseTickets(anyString(), anyInt());
  }

  @Test
  void retryWithSameIdempotencyKeyReturnsOriginalTickets() {
    ReservationEntity reservation =
      reservation(ReservationStatus.CONFIRMED, OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5));
    OffsetDateTime confirmedAt = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);
    reservation.confirm("PAY_123", "key-1", confirmedAt);

    List<TicketEntity> existing =
      List.of(
        new TicketEntity("tkt_a", event, reservation, TicketStatus.ISSUED, confirmedAt),
        new TicketEntity("tkt_b", event, reservation, TicketStatus.ISSUED, confirmedAt));
    when(ticketRepository.findByReservationReservationIdOrderByTicketId(RESERVATION_ID))
      .thenReturn(existing);

    ConfirmReservationResponse response =
      reservationService.confirmReservation(RESERVATION_ID, request("key-1"));

    assertEquals(
      List.of(new IssuedTicket("tkt_a"), new IssuedTicket("tkt_b")), response.tickets());
    assertEquals(confirmedAt, response.confirmedAt());
    verify(ticketRepository, never()).saveAll(anyList());
  }

  @Test
  void confirmWithDifferentKeyAfterConfirmationIsRejected() {
    ReservationEntity reservation =
      reservation(ReservationStatus.CONFIRMED, OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5));
    reservation.confirm("PAY_123", "key-1", OffsetDateTime.now(ZoneOffset.UTC));

    ReservationConfirmationException exception =
      assertThrows(
        ReservationConfirmationException.class,
        () -> reservationService.confirmReservation(RESERVATION_ID, request("key-2")));

    assertEquals(ConfirmationError.ALREADY_CONFIRMED, exception.getError());
    verify(ticketRepository, never()).saveAll(anyList());
  }

  @Test
  void confirmWithMissingFieldsIsRejected() {
    assertThrows(
      IllegalArgumentException.class,
      () -> reservationService.confirmReservation(RESERVATION_ID, null));
    assertThrows(
      IllegalArgumentException.class,
      () ->
        reservationService.confirmReservation(
          RESERVATION_ID, new ConfirmReservationRequest(" ", "PAY_123", "key-1")));
    assertThrows(
      IllegalArgumentException.class,
      () ->
        reservationService.confirmReservation(
          RESERVATION_ID, new ConfirmReservationRequest(CUSTOMER_ID, null, "key-1")));
    assertThrows(
      IllegalArgumentException.class,
      () ->
        reservationService.confirmReservation(
          RESERVATION_ID, new ConfirmReservationRequest(CUSTOMER_ID, "PAY_123", "")));

    verify(reservationRepository, never()).findByIdForUpdate(any());
  }
}
