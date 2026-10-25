package edu.columbia.coms4156.ticketing.repository;

import edu.columbia.coms4156.ticketing.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Provides database access to ticket reservations. */
@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, String> {}
