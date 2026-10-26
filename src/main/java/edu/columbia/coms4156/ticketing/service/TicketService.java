/* AI Generated */

package edu.columbia.coms4156.ticketing.service;

import edu.columbia.coms4156.ticketing.pojo.Ticket;
import org.springframework.stereotype.Service;

/** Sample service showing where application logic will belong. */
@Service
public class TicketService {

  /**
   * Provides a fixed ticket for the sample endpoint.
   *
   * @return example ticket data without database access or validation logic
   */
  public Ticket getSampleTicket() {
    return new Ticket("tkt-abc123", "evt-abc123", "res-123abc");
  }
}
