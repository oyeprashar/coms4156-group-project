package edu.columbia.coms4156.ticketing.service;

/** Reasons a reservation confirmation can be rejected. */
public enum ConfirmationError {
  RESERVATION_NOT_FOUND,
  RESERVATION_NOT_OWNED,
  RESERVATION_EXPIRED,
  RESERVATION_CANCELLED,
  ALREADY_CONFIRMED
}
