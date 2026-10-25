package edu.columbia.coms4156.ticketing.repository;

import edu.columbia.coms4156.ticketing.entity.ReservationEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Provides database access to ticket reservations. */
@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, String> {

  /**
   * Loads a reservation and locks its row until the transaction ends, so concurrent requests that
   * change its status are applied one at a time.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from ReservationEntity r where r.reservationId = :reservationId")
  Optional<ReservationEntity> findByIdForUpdate(@Param("reservationId") String reservationId);
}
