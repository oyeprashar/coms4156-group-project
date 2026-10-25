package edu.columbia.coms4156.ticketing.service;

/** Exception for when reservation cannot be cancelled */
public class ReservationCancellationException extends RuntimeException {
    private final CancellationError error;
    private final String reservationId;

    /* Creates an exception for a rejected cancellation of the reservation */
    public ReservationCancellationException(CancellationError error, String reservationId) {
        super(error.name());
        this.error = error;
        this.reservationId = reservationId;
    }

    public CancellationError getError() {
        return error;
    }

    public String getReservationId() {
        return reservationId;
    }
}
