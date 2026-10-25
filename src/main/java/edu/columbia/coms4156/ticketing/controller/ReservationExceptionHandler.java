package edu.columbia.coms4156.ticketing.controller;

import edu.columbia.coms4156.ticketing.service.InsufficientAvailabilityException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Handles errors raised by the reservation API. */
@RestControllerAdvice(assignableTypes = ReservationController.class)
public class ReservationExceptionHandler {

  /** Returns a conflict response when too few tickets are available. */
  @ExceptionHandler(InsufficientAvailabilityException.class)
  public ResponseEntity<Map<String, Object>> handleInsufficientAvailability(
      InsufficientAvailabilityException exception) {

    Map<String, Object> body =
        Map.of(
            "error", "INSUFFICIENT_AVAILABILITY",
            "requestedQuantity", exception.getRequestedQuantity(),
            "availableQuantity", exception.getAvailableQuantity());

    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  /** Returns a bad request response for invalid reservation inputs. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleInvalidRequest(
      IllegalArgumentException exception) {

    Map<String, String> body =
        Map.of("error", "INVALID_REQUEST", "message", exception.getMessage());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }
}
