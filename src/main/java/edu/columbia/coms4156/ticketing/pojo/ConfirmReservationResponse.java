package edu.columbia.coms4156.ticketing.pojo;

import java.time.OffsetDateTime;
import java.util.List;

/** Represents a confirmed reservation and the tickets issued for it. */
public record ConfirmReservationResponse(
    String reservationId,
    String eventId,
    String status,
    List<IssuedTicket> tickets,
    OffsetDateTime confirmedAt) {}
