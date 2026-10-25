package edu.columbia.coms4156.ticketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** Represents a hold on tickets for an event, which can later be confirmed. */
@Entity
@Table(name = "reservations")
public class ReservationEntity {

  @Id
  @Column(name = "reservation_id")
  private String reservationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private EventEntity event;

  @Column(name = "customer_id", nullable = false)
  private String customerId;

  private int quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReservationStatus status;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  @Column(name = "payment_reference")
  private String paymentReference;

  @Column(name = "confirm_idempotency_key")
  private String confirmIdempotencyKey;

  @Column(name = "confirmed_at")
  private OffsetDateTime confirmedAt;

  /** Required by JPA. */
  protected ReservationEntity() {}

  /** Creates a reservation for a customer holding tickets until the expiry time. */
  public ReservationEntity(
      String reservationId,
      EventEntity event,
      String customerId,
      int quantity,
      ReservationStatus status,
      OffsetDateTime expiresAt) {

    this.reservationId = reservationId;
    this.event = event;
    this.customerId = customerId;
    this.quantity = quantity;
    this.status = status;
    this.expiresAt = expiresAt;
  }

  /** Marks the reservation confirmed and records the payment and idempotency details. */
  public void confirm(
      String paymentReference, String confirmIdempotencyKey, OffsetDateTime confirmedAt) {

    this.status = ReservationStatus.CONFIRMED;
    this.paymentReference = paymentReference;
    this.confirmIdempotencyKey = confirmIdempotencyKey;
    this.confirmedAt = confirmedAt;
  }

  public String getReservationId() {
    return reservationId;
  }

  public EventEntity getEvent() {
    return event;
  }

  public String getCustomerId() {
    return customerId;
  }

  public int getQuantity() {
    return quantity;
  }

  public ReservationStatus getStatus() {
    return status;
  }

  public void setStatus(ReservationStatus status) {
    this.status = status;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public String getPaymentReference() {
    return paymentReference;
  }

  public String getConfirmIdempotencyKey() {
    return confirmIdempotencyKey;
  }

  public OffsetDateTime getConfirmedAt() {
    return confirmedAt;
  }
}
