package edu.columbia.coms4156.ticketing.service;

/** Indicates that a reservation requests more tickets than are available. */
public class InsufficientAvailabilityException extends RuntimeException {

  private final int requestedQuantity;
  private final int availableQuantity;

  /** Creates an exception with the requested and available ticket quantities. */
  public InsufficientAvailabilityException(int requestedQuantity, int availableQuantity) {
    super("INSUFFICIENT_AVAILABILITY");
    this.requestedQuantity = requestedQuantity;
    this.availableQuantity = availableQuantity;
  }

  public int getRequestedQuantity() {
    return requestedQuantity;
  }

  public int getAvailableQuantity() {
    return availableQuantity;
  }
}
