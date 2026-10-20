package edu.columbia.coms4156.ticketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Tracks total and available ticket inventory for an event. */
@Entity
@Table(name = "inventory")
public class InventoryEntity {

  @Id
  @Column(name = "event_id")
  private String eventId;

  @OneToOne
  @MapsId
  @JoinColumn(name = "event_id")
  private EventEntity event;

  @Column(name = "total_tickets")
  private int totalTickets;

  @Column(name = "available_tickets")
  private int availableTickets;

  /** Required by JPA. */
  protected InventoryEntity() {}

  /** Creates inventory for an event. */
  public InventoryEntity(EventEntity event, int totalTickets, int availableTickets) {

    this.event = event;
    this.totalTickets = totalTickets;
    this.availableTickets = availableTickets;
  }

  public String getEventId() {
    return eventId;
  }

  public EventEntity getEvent() {
    return event;
  }

  public int getTotalTickets() {
    return totalTickets;
  }

  public int getAvailableTickets() {
    return availableTickets;
  }

  public void setAvailableTickets(int availableTickets) {
    this.availableTickets = availableTickets;
  }
}
