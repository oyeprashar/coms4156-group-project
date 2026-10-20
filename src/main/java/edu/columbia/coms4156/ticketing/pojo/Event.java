package edu.columbia.coms4156.ticketing.pojo;

import java.time.OffsetDateTime;

/** Represents an event returned by the search API. */
public record Event(
    String eventId,
    String name,
    String category,
    Venue venue,
    OffsetDateTime startTime,
    TicketAvailability ticketAvailability,
    PriceRange priceRange) {}
