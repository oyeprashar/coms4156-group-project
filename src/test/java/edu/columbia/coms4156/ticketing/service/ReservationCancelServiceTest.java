package edu.columbia.coms4156.ticketing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import edu.columbia.coms4156.ticketing.entity.InventoryEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationStatus;
import edu.columbia.coms4156.ticketing.entity.VenueEntity;
import edu.columbia.coms4156.ticketing.pojo.CancelReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.CancelReservationResponse;
import edu.columbia.coms4156.ticketing.repository.EventRepository;
import edu.columbia.coms4156.ticketing.repository.InventoryRepository;
import edu.columbia.coms4156.ticketing.repository.ReservationRepository;
import edu.columbia.coms4156.ticketing.repository.TicketRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for cancelling reservations in ReservationService. */
@ExtendWith(MockitoExtension.class)
class ReservationCancelServiceTest {
  private static final String RESERVATION_ID = "res_001";
  private static final String CUSTOMER_ID = "cust_001";

  @Mock
  private EventRepository eventRepository;
  @Mock
  private ReservationRepository reservationRepository;
  @Mock
  private TicketRepository ticketRepository;
  @Mock
  private InventoryRepository inventoryRepository;

  private ReservationService reservationService;
  private EventEntity event;

  @BeforeEach
  void setUp() {
    reservationService = new ReservationService(
        eventRepository,
        inventoryRepository,
        reservationRepository,
        ticketRepository);
    VenueEntity venue = new VenueEntity("venue_001", "Madison Square Garden", "New York");
    event = new EventEntity(
        "evt_001",
        "Dom Dolla Live",
        "concert",
        OffsetDateTime.now(ZoneOffset.UTC).plusDays(30),
        new BigDecimal("85.00"),
        new BigDecimal("240.00"),
        "USD",
        venue);
  }

  private ReservationEntity reservation(ReservationStatus status, OffsetDateTime expiresAt) {
    ReservationEntity reservation = new ReservationEntity(
        RESERVATION_ID,
        event,
        CUSTOMER_ID,
        2,
        status,
        expiresAt);
    when(reservationRepository.findByIdForUpdate(RESERVATION_ID))
        .thenReturn(Optional.of(reservation));
    return reservation;
  }

  private ReservationEntity heldReservation() {
    return reservation(ReservationStatus.HELD, OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10));
  }

  @Test
  void cancelHeldReservationSetsStatusAndReleasesInventory() {
    final ReservationEntity reservation = heldReservation();
    InventoryEntity inventory = new InventoryEntity(event, 1000, 842);

    when(inventoryRepository.findById("evt_001")).thenReturn(Optional.of(inventory));

    CancelReservationResponse response = reservationService.cancelReservation(
        RESERVATION_ID, new CancelReservationRequest(CUSTOMER_ID));

    assertEquals(RESERVATION_ID, response.reservationId());
    assertEquals("evt_001", response.eventId());
    assertEquals("CANCELLED", response.status());
    assertEquals(2, response.releasedTickets());
    assertEquals(842, response.availableTickets());
    assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
    verify(inventoryRepository).releaseTickets("evt_001", 2);
    verify(reservationRepository).save(reservation);
  }

  @Test
  void cancelMissingReservationIsRejected() {
    when(reservationRepository.findByIdForUpdate(RESERVATION_ID)).thenReturn(Optional.empty());

    ReservationCancellationException exception = assertThrows(
        ReservationCancellationException.class,
        () -> reservationService.cancelReservation(
            RESERVATION_ID, new CancelReservationRequest(CUSTOMER_ID)));

    assertEquals(CancellationError.RESERVATION_NOT_FOUND, exception.getError());
    assertEquals(RESERVATION_ID, exception.getReservationId());
    verify(inventoryRepository, never()).releaseTickets(anyString(), anyInt());
    verify(reservationRepository, never()).save(any());
  }

  @Test
  void cancelByAnotherCustomerIsRejected() {
    heldReservation();

    ReservationCancellationException exception = assertThrows(
        ReservationCancellationException.class,
        () -> reservationService.cancelReservation(
            RESERVATION_ID, new CancelReservationRequest("cust_999")));

    assertEquals(CancellationError.RESERVATION_NOT_OWNED, exception.getError());
    assertEquals(RESERVATION_ID, exception.getReservationId());
    verify(inventoryRepository, never()).releaseTickets(anyString(), anyInt());
    verify(reservationRepository, never()).save(any());
  }

  @Test
  void cancelAlreadyConfirmedIsRejected() {
    reservation(ReservationStatus.CONFIRMED, null);

    ReservationCancellationException exception = assertThrows(
        ReservationCancellationException.class,
        () -> reservationService.cancelReservation(
            RESERVATION_ID, new CancelReservationRequest(CUSTOMER_ID)));

    assertEquals(CancellationError.RESERVATION_ALREADY_CONFIRMED, exception.getError());
    assertEquals(RESERVATION_ID, exception.getReservationId());
    verify(inventoryRepository, never()).releaseTickets(anyString(), anyInt());
    verify(reservationRepository, never()).save(any());
  }

  @Test
  void cancelWithMissingCustomerIdIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> reservationService.cancelReservation(RESERVATION_ID, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> reservationService.cancelReservation(
            RESERVATION_ID, new CancelReservationRequest("")));
    assertThrows(
        IllegalArgumentException.class,
        () -> reservationService.cancelReservation(
            RESERVATION_ID, new CancelReservationRequest("   ")));
    verify(reservationRepository, never()).findByIdForUpdate(anyString());
  }
}
