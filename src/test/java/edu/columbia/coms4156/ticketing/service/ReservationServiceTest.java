package edu.columbia.coms4156.ticketing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import edu.columbia.coms4156.ticketing.entity.InventoryEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationEntity;
import edu.columbia.coms4156.ticketing.pojo.ReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.ReservationResponse;
import edu.columbia.coms4156.ticketing.repository.EventRepository;
import edu.columbia.coms4156.ticketing.repository.InventoryRepository;
import edu.columbia.coms4156.ticketing.repository.ReservationRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

  @Mock private EventRepository eventRepository;

  @Mock private InventoryRepository inventoryRepository;

  @Mock private ReservationRepository reservationRepository;

  @Mock private EventEntity event;

  @Mock private InventoryEntity inventory;

  private ReservationService reservationService;

  @BeforeEach
  void setUp() {
    reservationService =
        new ReservationService(eventRepository, inventoryRepository, reservationRepository);
  }

  /** Configures a valid event with available inventory. */
  private void setUpAvailableInventory(int availableTickets) {
    when(eventRepository.findById("evt_001")).thenReturn(Optional.of(event));

    when(inventoryRepository.findByEventIdForUpdate("evt_001")).thenReturn(Optional.of(inventory));

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
    when(eventRepository.findById("evt_001")).thenReturn(Optional.of(event));

    when(inventoryRepository.findByEventIdForUpdate("evt_001")).thenReturn(Optional.empty());

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
    assertEquals("HELD", savedReservation.getStatus());
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
}
