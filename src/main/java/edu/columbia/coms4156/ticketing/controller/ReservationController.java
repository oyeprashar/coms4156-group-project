package edu.columbia.coms4156.ticketing.controller;

import edu.columbia.coms4156.ticketing.pojo.CancelReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.CancelReservationResponse;
import edu.columbia.coms4156.ticketing.pojo.ConfirmReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.ConfirmReservationResponse;
import edu.columbia.coms4156.ticketing.pojo.ReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.ReservationResponse;
import edu.columbia.coms4156.ticketing.service.ReservationCancellationException;
import edu.columbia.coms4156.ticketing.service.ReservationConfirmationException;
import edu.columbia.coms4156.ticketing.service.ReservationService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Handles HTTP requests for ticket reservations and confirmations. */
@RestController
@RequestMapping("/v1/reservations")
public class ReservationController {

  private final ReservationService reservationService;

  /**
   * Creates the reservation controller.
   *
   * @param reservationService service for reservation operations
   */
  public ReservationController(ReservationService reservationService) {
    this.reservationService = reservationService;
  }

  /** Creates a new ticket reservation. */
  @PostMapping
  public ResponseEntity<ReservationResponse> reserveTickets(
      @RequestBody ReservationRequest request) {
    ReservationResponse response = reservationService.reserveTickets(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** Confirms a held reservation and returns the issued tickets. */
  @PostMapping("/{reservationId}/confirm")
  public ConfirmReservationResponse confirmReservation(
      @PathVariable String reservationId, @RequestBody ConfirmReservationRequest request) {
    return reservationService.confirmReservation(reservationId, request);
  }

  /** Cancels a held reservation and releases its tickets back into inventory. */
  @PostMapping("/{reservationId}/cancel")
  public CancelReservationResponse cancelReservation(
      @PathVariable String reservationId, @RequestBody CancelReservationRequest request) {
    return reservationService.cancelReservation(reservationId, request);
  }

  /** Maps rejected cancellations to an HTTP status and error code. */
  @ExceptionHandler(ReservationCancellationException.class)
  public ResponseEntity<Map<String, String>> handleCancellationError(
      ReservationCancellationException exception) {
    HttpStatus status = switch (exception.getError()) {
      case RESERVATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case RESERVATION_NOT_OWNED -> HttpStatus.FORBIDDEN;
      case RESERVATION_ALREADY_CONFIRMED -> HttpStatus.CONFLICT;
    };

    return ResponseEntity.status(status)
        .body(
            Map.of(
                "error", exception.getError().name(),
                "reservationId", exception.getReservationId()));
  }

  /** Maps rejected confirmations to an HTTP status and error code. */
  @ExceptionHandler(ReservationConfirmationException.class)
  public ResponseEntity<Map<String, String>> handleConfirmationError(
      ReservationConfirmationException exception) {

    HttpStatus status = switch (exception.getError()) {
      case RESERVATION_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case RESERVATION_NOT_OWNED -> HttpStatus.FORBIDDEN;
      case RESERVATION_EXPIRED, RESERVATION_CANCELLED, ALREADY_CONFIRMED -> HttpStatus.CONFLICT;
    };

    return ResponseEntity.status(status)
        .body(
            Map.of(
                "error", exception.getError().name(),
                "reservationId", exception.getReservationId()));
  }

  /** Returns HTTP 400 for missing request fields. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleInvalidRequest(
      IllegalArgumentException exception) {

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", exception.getMessage()));
  }
}
