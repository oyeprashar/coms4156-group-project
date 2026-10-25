package edu.columbia.coms4156.ticketing.repository;

import edu.columbia.coms4156.ticketing.entity.TicketEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Provides database access to issued tickets. */
@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, String> {

  /** Returns the tickets issued for a reservation, ordered by ticket ID. */
  List<TicketEntity> findByReservationReservationIdOrderByTicketId(String reservationId);
}
