package edu.columbia.coms4156.ticketing.pojo;

/**
 * Request body for cancelling a reservation.
 *
 * @param customerId customer that owns the reservation
 */
public record CancelReservationRequest(String customerId) {
}