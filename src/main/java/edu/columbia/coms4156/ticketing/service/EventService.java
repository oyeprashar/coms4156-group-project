package edu.columbia.coms4156.ticketing.service;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import edu.columbia.coms4156.ticketing.repository.EventRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** Handles event searching, filtering, and pagination. */
@Service
public class EventService {

  private final EventRepository eventRepository;

  /** Initializes the event service with its repository. */
  public EventService(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  /**
   * Finds upcoming events matching the supplied filters. Dates are interpreted as UTC calendar
   * dates.
   */
  public Page<EventEntity> searchEvents(
      String city,
      String category,
      LocalDate startDate,
      LocalDate endDate,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      String keyword,
      int page,
      int pageSize) {

    validateFilters(startDate, endDate, minPrice, maxPrice, page, pageSize);

    Specification<EventEntity> specification =
        (root, query, cb) -> {
          List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

          predicates.add(cb.greaterThan(root.get("startTime"), OffsetDateTime.now(ZoneOffset.UTC)));

          if (city != null && !city.isBlank()) {
            predicates.add(
                cb.equal(
                    cb.lower(root.get("venue").get("city")),
                    city.trim().toLowerCase(java.util.Locale.ROOT)));
          }

          if (category != null && !category.isBlank()) {
            predicates.add(
                cb.equal(
                    cb.lower(root.get("category")),
                    category.trim().toLowerCase(java.util.Locale.ROOT)));
          }

          if (startDate != null) {
            predicates.add(
                cb.greaterThanOrEqualTo(
                    root.get("startTime"), startDate.atStartOfDay().atOffset(ZoneOffset.UTC)));
          }

          if (endDate != null) {
            predicates.add(
                cb.lessThan(
                    root.get("startTime"),
                    endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)));
          }

          if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("maxPrice"), minPrice));
          }

          if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("minPrice"), maxPrice));
          }

          if (keyword != null && !keyword.isBlank()) {
            predicates.add(
                cb.like(
                    cb.lower(root.get("name")),
                    "%"
                        + keyword
                            .trim()
                            .toLowerCase(java.util.Locale.ROOT)
                            .replace("\\", "\\\\")
                            .replace("%", "\\%")
                            .replace("_", "\\_")
                        + "%",
                    '\\'));
          }

          return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

    PageRequest pageable =
        PageRequest.of(
            page, pageSize, Sort.by("startTime").ascending().and(Sort.by("eventId").ascending()));

    return eventRepository.findAll(specification, pageable);
  }

  /** Rejects invalid search parameters. */
  private void validateFilters(
      LocalDate startDate,
      LocalDate endDate,
      BigDecimal minPrice,
      BigDecimal maxPrice,
      int page,
      int pageSize) {

    if (page < 0 || pageSize < 1 || pageSize > 100) {
      throw new IllegalArgumentException("Invalid pagination parameters");
    }

    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
      throw new IllegalArgumentException("startDate cannot be after endDate");
    }

    if ((minPrice != null && minPrice.signum() < 0)
        || (maxPrice != null && maxPrice.signum() < 0)) {
      throw new IllegalArgumentException("Prices cannot be negative");
    }

    if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
      throw new IllegalArgumentException("minPrice cannot exceed maxPrice");
    }
  }
}
