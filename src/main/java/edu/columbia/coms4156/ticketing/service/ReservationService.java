package edu.columbia.coms4156.ticketing.service;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import edu.columbia.coms4156.ticketing.entity.InventoryEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationStatus;
import edu.columbia.coms4156.ticketing.entity.TicketEntity;
import edu.columbia.coms4156.ticketing.entity.TicketStatus;
import edu.columbia.coms4156.ticketing.pojo.ConfirmReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.ConfirmReservationResponse;
import edu.columbia.coms4156.ticketing.pojo.IssuedTicket;
import edu.columbia.coms4156.ticketing.pojo.ReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.ReservationResponse;
import edu.columbia.coms4156.ticketing.repository.EventRepository;
import edu.columbia.coms4156.ticketing.repository.InventoryRepository;
import edu.columbia.coms4156.ticketing.repository.ReservationRepository;
import edu.columbia.coms4156.ticketing.repository.TicketRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Handles ticket reservations, confirmations, and ticket issuance. */
@Service
public class ReservationService {

  private static final int HOLD_MINUTES = 10;

  private final EventRepository eventRepository;
  private final InventoryRepository inventoryRepository;
  private final ReservationRepository reservationRepository;
  private final TicketRepository ticketRepository;

  /** Initializes the reservation service with its repositories. */
  public ReservationService(
    EventRepository eventRepository,
    InventoryRepository inventoryRepository,
    ReservationRepository reservationRepository,
    TicketRepository ticketRepository) {
    this.eventRepository = eventRepository;
    this.inventoryRepository = inventoryRepository;
    this.reservationRepository = reservationRepository;
    this.ticketRepository = ticketRepository;
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

  /**
   * Confirms a held reservation and issues one ticket per reserved seat. The reservation row is
   * locked for the whole transaction, so concurrent confirmations cannot issue tickets twice.
   *
   * <p>A hold past its expiry is marked EXPIRED and its tickets go back into inventory before the
   * request is rejected; that change is committed even though the request fails. Repeating a
   * successful confirmation with the same idempotency key returns the original result.
   */
  @Transactional(noRollbackFor = ReservationConfirmationException.class)
  public ConfirmReservationResponse confirmReservation(
    String reservationId, ConfirmReservationRequest request) {

    validateRequest(request);

    ReservationEntity reservation =
      reservationRepository
        .findByIdForUpdate(reservationId)
        .orElseThrow(
          () ->
            new ReservationConfirmationException(
              ConfirmationError.RESERVATION_NOT_FOUND, reservationId));

    if (!reservation.getCustomerId().equals(request.customerId())) {
      throw new ReservationConfirmationException(
        ConfirmationError.RESERVATION_NOT_OWNED, reservationId);
    }

    switch (reservation.getStatus()) {
      case CONFIRMED -> {
        if (request.idempotencyKey().equals(reservation.getConfirmIdempotencyKey())) {
          return toResponse(
            reservation,
            ticketRepository.findByReservationReservationIdOrderByTicketId(reservationId));
        }
        throw new ReservationConfirmationException(
          ConfirmationError.ALREADY_CONFIRMED, reservationId);
      }
      case EXPIRED ->
        throw new ReservationConfirmationException(
          ConfirmationError.RESERVATION_EXPIRED, reservationId);
      case CANCELLED ->
        throw new ReservationConfirmationException(
          ConfirmationError.RESERVATION_CANCELLED, reservationId);
      case HELD -> {
        // Continue below.
      }
      default -> throw new IllegalStateException("Unknown status " + reservation.getStatus());
    }

    // Databases store microseconds, so truncate to keep retried responses identical.
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

    if (!now.isBefore(reservation.getExpiresAt())) {
      reservation.setStatus(ReservationStatus.EXPIRED);
      reservationRepository.save(reservation);
      inventoryRepository.releaseTickets(
        reservation.getEvent().getEventId(), reservation.getQuantity());
      throw new ReservationConfirmationException(
        ConfirmationError.RESERVATION_EXPIRED, reservationId);
    }

    reservation.confirm(request.paymentReference(), request.idempotencyKey(), now);
    reservationRepository.save(reservation);

    List<TicketEntity> tickets = new ArrayList<>();
    for (int i = 0; i < reservation.getQuantity(); i++) {
      tickets.add(
        new TicketEntity(
          "tkt_" + UUID.randomUUID(),
          reservation.getEvent(),
          reservation,
          TicketStatus.ISSUED,
          now));
    }

    return toResponse(reservation, ticketRepository.saveAll(tickets));
  }

  /** Rejects confirmation requests with missing fields. */
  private void validateRequest(ConfirmReservationRequest request) {
    if (request == null
      || isBlank(request.customerId())
      || isBlank(request.paymentReference())
      || isBlank(request.idempotencyKey())) {
      throw new IllegalArgumentException(
        "customerId, paymentReference, and idempotencyKey are required");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  /** Converts a confirmed reservation and its tickets into the public API response. */
  private ConfirmReservationResponse toResponse(
    ReservationEntity reservation, List<TicketEntity> tickets) {

    List<IssuedTicket> issued =
      tickets.stream()
        .map(ticket -> new IssuedTicket(ticket.getTicketId()))
        .sorted(Comparator.comparing(IssuedTicket::ticketId))
        .toList();

    return new ConfirmReservationResponse(
      reservation.getReservationId(),
      reservation.getEvent().getEventId(),
      reservation.getStatus().name(),
      issued,
      reservation.getConfirmedAt());
  }
}

