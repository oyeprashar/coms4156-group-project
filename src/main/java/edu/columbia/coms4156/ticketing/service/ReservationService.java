package edu.columbia.coms4156.ticketing.service;

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
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Handles ticket reservation operations. */
@Service
public class ReservationService {

  private static final int HOLD_MINUTES = 10;

  private final EventRepository eventRepository;
  private final InventoryRepository inventoryRepository;
  private final ReservationRepository reservationRepository;

  /** Initializes the reservation service with its repositories. */
  public ReservationService(
      EventRepository eventRepository,
      InventoryRepository inventoryRepository,
      ReservationRepository reservationRepository) {
    this.eventRepository = eventRepository;
    this.inventoryRepository = inventoryRepository;
    this.reservationRepository = reservationRepository;
  }

  /** Reserves available tickets for an event. */
  @Transactional
  public ReservationResponse reserveTickets(ReservationRequest request) {

    if (request == null
        || request.eventId() == null
        || request.eventId().isBlank()
        || request.customerId() == null
        || request.customerId().isBlank()
        || request.quantity() <= 0) {
      throw new IllegalArgumentException("Invalid reservation request");
    }

    EventEntity event =
        eventRepository
            .findById(request.eventId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

    InventoryEntity inventory =
        inventoryRepository
            .findByEventIdForUpdate(request.eventId())
            .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));

    if (inventory.getAvailableTickets() < request.quantity()) {
      throw new InsufficientAvailabilityException(
          request.quantity(), inventory.getAvailableTickets());
    }

    int remainingTickets = inventory.getAvailableTickets() - request.quantity();
    inventory.setAvailableTickets(remainingTickets);

    String reservationId = "res_" + UUID.randomUUID();
    OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(HOLD_MINUTES);

    ReservationEntity reservation =
        new ReservationEntity(
            reservationId, event, request.customerId(), request.quantity(), "HELD", expiresAt);

    inventoryRepository.save(inventory);
    reservationRepository.save(reservation);

    return new ReservationResponse(
        reservationId, request.eventId(), request.quantity(), "HELD", expiresAt, remainingTickets);
  }
}
