package edu.columbia.coms4156.ticketing.pojo;

import java.util.List;

/** Represents the results and total count of an event search. */
public record EventSearchResponse(List<Event> events, long count) {}
