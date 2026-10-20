
package edu.columbia.coms4156.ticketing.pojo;

import java.util.List;

public record EventSearchResponse(
  List<Event> events,
  long count) {
}
