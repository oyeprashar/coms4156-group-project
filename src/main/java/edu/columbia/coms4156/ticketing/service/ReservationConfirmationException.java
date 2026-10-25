package edu.columbia.coms4156.ticketing.service;

/** Thrown when a reservation cannot be confirmed. */
public class ReservationConfirmationException extends RuntimeException {

  private final ConfirmationError error;
  private final String reservationId;

  /** Creates an exception for a rejected confirmation of the given reservation. */
  public ReservationConfirmationException(ConfirmationError error, String reservationId) {
    super(error.name());
    this.error = error;
    this.reservationId = reservationId;
  }

  public ConfirmationError getError() {
    return error;
  }

  public String getReservationId() {
    return reservationId;
  }
}
