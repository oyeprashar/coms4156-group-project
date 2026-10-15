package edu.columbia.coms4156.ticketing.pojo;

/**
 * Simple ticket data used by the sample endpoint. Records provide immutable fields and accessors.
 *
 * @param ticketId ticket identifier
 * @param eventId event identifier
 * @param reservationId reservation identifier
 */
public record Ticket(String ticketId, String eventId, String reservationId) {}
