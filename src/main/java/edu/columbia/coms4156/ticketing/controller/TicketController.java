package edu.columbia.coms4156.ticketing.controller;

import edu.columbia.coms4156.ticketing.pojo.Ticket;
import edu.columbia.coms4156.ticketing.service.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sample controller showing route mapping and delegation to a service. */
@RestController
@RequestMapping("/v1")
public class TicketController {

  private final TicketService ticketService;

  /**
   * Creates the controller using constructor injection.
   *
   * @param ticketService service providing ticket data
   */
  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  /**
   * Returns a sample ticket as JSON.
   *
   * @return fixed example data for demonstrating the project structure
   */
  @GetMapping("/sample")
  public Ticket getSampleTicket() {
    return ticketService.getSampleTicket();
  }
}
