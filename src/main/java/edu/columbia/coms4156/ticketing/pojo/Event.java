
package edu.columbia.coms4156.ticketing.pojo;

import java.time.OffsetDateTime;

public record Event(
  String eventId,
  String name,
  String category,
  Venue venue,
  OffsetDateTime startTime,
  TicketAvailability ticketAvailability,
  PriceRange priceRange) {
}
