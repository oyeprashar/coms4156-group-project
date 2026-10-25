package edu.columbia.coms4156.ticketing.pojo;

/**
 * Request body for confirming a reservation.
 *
 * @param customerId customer that owns the reservation
 * @param paymentReference client-supplied payment reference, stored but not verified
 * @param idempotencyKey key that makes retried confirmations return the original result
 */
public record ConfirmReservationRequest(
    String customerId, String paymentReference, String idempotencyKey) {}
