package edu.columbia.coms4156.ticketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Represents a venue where events are hosted. */
@Entity
@Table(name = "venues")
public class VenueEntity {

  @Id
  @Column(name = "venue_id")
  private String venueId;

  private String name;

  private String city;

  /** Required by JPA. */
  protected VenueEntity() {}

  /** Creates a venue with its identifier, name, and city. */
  public VenueEntity(String venueId, String name, String city) {
    this.venueId = venueId;
    this.name = name;
    this.city = city;
  }

  public String getVenueId() {
    return venueId;
  }

  public String getName() {
    return name;
  }

  public String getCity() {
    return city;
  }
}
