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

/** Represents a single ticket issued for a confirmed reservation. */
@Entity
@Table(name = "tickets")
public class TicketEntity {

  @Id
  @Column(name = "ticket_id")
  private String ticketId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private EventEntity event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reservation_id", nullable = false)
  private ReservationEntity reservation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TicketStatus status;

  @Column(name = "issued_at", nullable = false)
  private OffsetDateTime issuedAt;

  /** Required by JPA. */
  protected TicketEntity() {}

  /** Creates a ticket issued against a reservation. */
  public TicketEntity(
      String ticketId,
      EventEntity event,
      ReservationEntity reservation,
      TicketStatus status,
      OffsetDateTime issuedAt) {

    this.ticketId = ticketId;
    this.event = event;
    this.reservation = reservation;
    this.status = status;
    this.issuedAt = issuedAt;
  }

  public String getTicketId() {
    return ticketId;
  }

  public EventEntity getEvent() {
    return event;
  }

  public ReservationEntity getReservation() {
    return reservation;
  }

  public TicketStatus getStatus() {
    return status;
  }

  public void setStatus(TicketStatus status) {
    this.status = status;
  }

  public OffsetDateTime getIssuedAt() {
    return issuedAt;
  }
}
