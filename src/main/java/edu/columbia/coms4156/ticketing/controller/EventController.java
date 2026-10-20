package edu.columbia.coms4156.ticketing.controller;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import edu.columbia.coms4156.ticketing.entity.InventoryEntity;
import edu.columbia.coms4156.ticketing.pojo.Event;
import edu.columbia.coms4156.ticketing.pojo.EventSearchResponse;
import edu.columbia.coms4156.ticketing.pojo.PriceRange;
import edu.columbia.coms4156.ticketing.pojo.TicketAvailability;
import edu.columbia.coms4156.ticketing.pojo.Venue;
import edu.columbia.coms4156.ticketing.repository.InventoryRepository;
import edu.columbia.coms4156.ticketing.service.EventService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for searching upcoming events. */
@RestController
@RequestMapping("/v1/events")
public class EventController {

  private final EventService eventService;
  private final InventoryRepository inventoryRepository;

  /** Initializes the event controller with its dependencies. */
  public EventController(EventService eventService, InventoryRepository inventoryRepository) {
    this.eventService = eventService;
    this.inventoryRepository = inventoryRepository;
  }

  /** Returns events matching optional search filters. */
  @GetMapping
  public EventSearchResponse searchEvents(
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @RequestParam(required = false) BigDecimal minPrice,
      @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int pageSize) {

    Page<EventEntity> results =
        eventService.searchEvents(
            city, category, startDate, endDate, minPrice, maxPrice, keyword, page, pageSize);

    List<String> eventIds = results.getContent().stream().map(EventEntity::getEventId).toList();

    Map<String, InventoryEntity> inventoryMap =
        inventoryRepository.findAllById(eventIds).stream()
            .collect(Collectors.toMap(InventoryEntity::getEventId, Function.identity()));

    List<Event> events =
        results.getContent().stream()
            .map(entity -> toEvent(entity, inventoryMap.get(entity.getEventId())))
            .toList();

    return new EventSearchResponse(events, results.getTotalElements());
  }

  /** Converts database records into the public API response. */
  private Event toEvent(EventEntity entity, InventoryEntity inventory) {

    var venueEntity = entity.getVenue();

    Venue venue = new Venue(venueEntity.getVenueId(), venueEntity.getName(), venueEntity.getCity());

    PriceRange priceRange =
        new PriceRange(entity.getMinPrice(), entity.getMaxPrice(), entity.getCurrency());

    int availableCount = inventory == null ? 0 : inventory.getAvailableTickets();

    TicketAvailability availability = new TicketAvailability(availableCount > 0, availableCount);

    return new Event(
        entity.getEventId(),
        entity.getName(),
        entity.getCategory(),
        venue,
        entity.getStartTime(),
        availability,
        priceRange);
  }

  /** Returns HTTP 400 for invalid search filters. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleInvalidFilters(
      IllegalArgumentException exception) {

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Map.of("error", exception.getMessage()));
  }
}
