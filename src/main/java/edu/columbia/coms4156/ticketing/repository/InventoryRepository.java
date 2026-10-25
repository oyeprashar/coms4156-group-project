package edu.columbia.coms4156.ticketing.repository;

import edu.columbia.coms4156.ticketing.entity.InventoryEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Provides database access to event ticket inventory. */
@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, String> {
  /** Retrieves inventory while locking it against concurrent modifications. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM InventoryEntity i WHERE i.eventId = :eventId")
  Optional<InventoryEntity> findByEventIdForUpdate(@Param("eventId") String eventId);
  /**
   * Atomically returns held tickets to an event's available inventory.
   *
   * @return the number of inventory rows updated
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
    "update InventoryEntity i set i.availableTickets = i.availableTickets + :quantity"
      + " where i.eventId = :eventId")
  int releaseTickets(@Param("eventId") String eventId, @Param("quantity") int quantity);
}
