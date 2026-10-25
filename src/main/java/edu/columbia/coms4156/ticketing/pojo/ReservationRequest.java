package edu.columbia.coms4156.ticketing.pojo;

/** Represents a request to reserve tickets for an event. */
public record ReservationRequest(String eventId, int quantity, String customerId) {}
