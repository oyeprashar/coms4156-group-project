
package edu.columbia.coms4156.ticketing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import edu.columbia.coms4156.ticketing.repository.EventRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

/**
 * Unit tests for EventService.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

  @Mock
  private EventRepository eventRepository;

  private EventService eventService;

  @BeforeEach
  void setUp() {
    eventService = new EventService(eventRepository);
  }

  private Page<EventEntity> search(
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    int page,
    int pageSize) {

    return eventService.searchEvents(
      null,
      null,
      startDate,
      endDate,
      minPrice,
      maxPrice,
      null,
      page,
      pageSize);
  }

  @Test
  void searchEventsReturnsRepositoryResults() {
    Page<EventEntity> expected =
      new PageImpl<>(List.of());

    when(eventRepository.findAll(
      any(Specification.class),
      any(Pageable.class)))
      .thenReturn(expected);

    Page<EventEntity> actual = search(
      null, null, null, null, 0, 10);

    assertEquals(expected, actual);
    assertTrue(actual.isEmpty());

    verify(eventRepository).findAll(
      any(Specification.class),
      any(Pageable.class));
  }

  @Test
  void searchEventsUsesCorrectPagination() {
    when(eventRepository.findAll(
      any(Specification.class),
      any(Pageable.class)))
      .thenReturn(Page.empty());

    search(null, null, null, null, 2, 5);

    ArgumentCaptor<Pageable> captor =
      ArgumentCaptor.forClass(Pageable.class);

    verify(eventRepository).findAll(
      any(Specification.class),
      captor.capture());

    assertEquals(2, captor.getValue().getPageNumber());
    assertEquals(5, captor.getValue().getPageSize());
    assertEquals("startTime: ASC,eventId: ASC",
      captor.getValue().getSort().toString());
  }

  @Test
  void searchEventsRejectsNegativePage() {
    assertThrows(IllegalArgumentException.class,
      () -> search(
        null, null, null, null, -1, 10));
  }

  @Test
  void searchEventsRejectsZeroPageSize() {
    assertThrows(IllegalArgumentException.class,
      () -> search(
        null, null, null, null, 0, 0));
  }

  @Test
  void searchEventsRejectsPageSizeOver100() {
    assertThrows(IllegalArgumentException.class,
      () -> search(
        null, null, null, null, 0, 101));
  }

  @Test
  void searchEventsRejectsInvalidDateRange() {
    LocalDate start = LocalDate.of(2027, 5, 1);
    LocalDate end = LocalDate.of(2026, 11, 1);

    assertThrows(IllegalArgumentException.class,
      () -> search(
        start, end, null, null, 0, 10));
  }

  @Test
  void searchEventsRejectsNegativeMinPrice() {
    assertThrows(IllegalArgumentException.class,
      () -> search(
        null, null,
        new BigDecimal("-10"), null, 0, 10));
  }

  @Test
  void searchEventsRejectsNegativeMaxPrice() {
    assertThrows(IllegalArgumentException.class,
      () -> search(
        null, null,
        null, new BigDecimal("-10"), 0, 10));
  }

  @Test
  void searchEventsRejectsInvalidPriceRange() {
    assertThrows(IllegalArgumentException.class,
      () -> search(
        null, null,
        new BigDecimal("300"),
        new BigDecimal("100"), 0, 10));
  }

  @Test
  void searchEventsAcceptsValidBoundaries() {
    when(eventRepository.findAll(
      any(Specification.class),
      any(Pageable.class)))
      .thenReturn(Page.empty());

    Page<EventEntity> result = search(
      LocalDate.of(2026, 11, 1),
      LocalDate.of(2026, 11, 1),
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      0,
      100);

    assertTrue(result.isEmpty());
  }
}
