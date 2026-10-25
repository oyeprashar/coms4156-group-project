package edu.columbia.coms4156.ticketing.controller;

import edu.columbia.coms4156.ticketing.pojo.ReservationRequest;
import edu.columbia.coms4156.ticketing.pojo.ReservationResponse;
import edu.columbia.coms4156.ticketing.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Handles HTTP requests for ticket reservations. */
@RestController
@RequestMapping("/v1/reservations")
public class ReservationController {

  private final ReservationService reservationService;

  /** Initializes the controller with its reservation service. */
  public ReservationController(ReservationService reservationService) {
    this.reservationService = reservationService;
  }

  /** Creates a new ticket reservation. */
  @PostMapping
  public ResponseEntity<ReservationResponse> reserveTickets(
      @RequestBody ReservationRequest request) {
    ReservationResponse response = reservationService.reserveTickets(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
