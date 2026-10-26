package edu.columbia.coms4156.ticketing.pojo;

/**
 * Response body for a cancelled reservation.
 *
 * @param reservationId    the cancelled reservation's ID
 * @param eventId          the event the reservation was for
 * @param status           updated reservation status
 * @param releasedTickets  number of tickets returned to inventory
 * @param availableTickets updated available ticket count for the event
 */
public record CancelReservationResponse(
    String reservationId,
    String eventId,
    String status,
    int releasedTickets,
    int availableTickets) {
}