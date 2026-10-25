package edu.columbia.coms4156.ticketing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationStatus;
import edu.columbia.coms4156.ticketing.entity.TicketEntity;
import edu.columbia.coms4156.ticketing.entity.TicketStatus;
import edu.columbia.coms4156.ticketing.entity.VenueEntity;
import edu.columbia.coms4156.ticketing.pojo.ConfirmReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.ConfirmReservationResponse;
import edu.columbia.coms4156.ticketing.pojo.IssuedTicket;
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

/** Unit tests for ReservationService. */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

  private static final String RESERVATION_ID = "res_001";
  private static final String CUSTOMER_ID = "cust_001";

  @Mock private ReservationRepository reservationRepository;
  @Mock private TicketRepository ticketRepository;
  @Mock private InventoryRepository inventoryRepository;

  private ReservationService reservationService;
  private EventEntity event;

  @BeforeEach
  void setUp() {
    reservationService =
        new ReservationService(reservationRepository, ticketRepository, inventoryRepository);

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
