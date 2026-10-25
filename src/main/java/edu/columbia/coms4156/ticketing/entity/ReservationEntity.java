package edu.columbia.coms4156.ticketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** Represents a customer's reservation of tickets for an event. */
@Entity
@Table(name = "reservations")
public class ReservationEntity {

  @Id
  @Column(name = "reservation_id", nullable = false)
  private String reservationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private EventEntity event;

  @Column(name = "customer_id", nullable = false)
  private String customerId;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false)
  private String status;

  @Column(name = "expires_at", nullable = false)
  private OffsetDateTime expiresAt;

  /** Required by JPA. */
  protected ReservationEntity() {}

  /** Creates a reservation with its initial status and expiration time. */
  public ReservationEntity(
      String reservationId,
      EventEntity event,
      String customerId,
      int quantity,
      String status,
      OffsetDateTime expiresAt) {
    this.reservationId = reservationId;
    this.event = event;
    this.customerId = customerId;
    this.quantity = quantity;
    this.status = status;
    this.expiresAt = expiresAt;
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

  public String getStatus() {
    return status;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
