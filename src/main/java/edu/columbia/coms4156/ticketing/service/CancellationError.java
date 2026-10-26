package edu.columbia.coms4156.ticketing.service;

/** Reasons a reservation cancellation can be rejected. */
public enum CancellationError {
  RESERVATION_NOT_FOUND,
  RESERVATION_NOT_OWNED,
  RESERVATION_ALREADY_CONFIRMED
}
