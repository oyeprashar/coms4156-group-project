package edu.columbia.coms4156.ticketing.service;

import edu.columbia.coms4156.ticketing.entity.ReservationEntity;
import edu.columbia.coms4156.ticketing.entity.ReservationStatus;
import edu.columbia.coms4156.ticketing.entity.TicketEntity;
import edu.columbia.coms4156.ticketing.entity.TicketStatus;
import edu.columbia.coms4156.ticketing.pojo.ConfirmReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.ConfirmReservationResponse;
import edu.columbia.coms4156.ticketing.pojo.IssuedTicket;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Handles reservation state changes, including confirming holds and issuing tickets. */
@Service
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final TicketRepository ticketRepository;
  private final InventoryRepository inventoryRepository;

  /** Initializes the reservation service with its repositories. */
  public ReservationService(
      ReservationRepository reservationRepository,
      TicketRepository ticketRepository,
      InventoryRepository inventoryRepository) {

    this.reservationRepository = reservationRepository;
    this.ticketRepository = ticketRepository;
    this.inventoryRepository = inventoryRepository;
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
