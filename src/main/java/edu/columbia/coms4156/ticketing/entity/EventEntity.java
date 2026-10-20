package edu.columbia.coms4156.ticketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Represents an event available for ticket booking. */
@Entity
@Table(name = "events")
public class EventEntity {

  @Id
  @Column(name = "event_id")
  private String eventId;

  private String name;

  private String category;

  @Column(name = "start_time")
  private OffsetDateTime startTime;

  @Column(name = "min_price")
  private BigDecimal minPrice;

  @Column(name = "max_price")
  private BigDecimal maxPrice;

  private String currency;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "venue_id", nullable = false)
  private VenueEntity venue;

  /** Required by JPA. */
  protected EventEntity() {}

  /** Creates an event with its details, pricing, and venue. */
  public EventEntity(
      String eventId,
      String name,
      String category,
      OffsetDateTime startTime,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      String currency,
      VenueEntity venue) {

    this.eventId = eventId;
    this.name = name;
    this.category = category;
    this.startTime = startTime;
    this.minPrice = minPrice;
    this.maxPrice = maxPrice;
    this.currency = currency;
    this.venue = venue;
  }

  public String getEventId() {
    return eventId;
  }

  public String getName() {
    return name;
  }

  public String getCategory() {
    return category;
  }

  public OffsetDateTime getStartTime() {
    return startTime;
  }

  public BigDecimal getMinPrice() {
    return minPrice;
  }

  public BigDecimal getMaxPrice() {
    return maxPrice;
  }

  public String getCurrency() {
    return currency;
  }

  public VenueEntity getVenue() {
    return venue;
  }
}
