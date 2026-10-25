package edu.columbia.coms4156.ticketing.pojo;

import java.time.OffsetDateTime;

/** Represents the response after successfully reserving tickets. */
public record ReservationResponse(
    String reservationId,
    String eventId,
    int quantity,
    String status,
    OffsetDateTime expiresAt,
    int remainingTickets) {}
