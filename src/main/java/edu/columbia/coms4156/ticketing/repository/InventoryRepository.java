
package edu.columbia.coms4156.ticketing.repository;

import edu.columbia.coms4156.ticketing.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Provides database access to event ticket inventory.
 */
@Repository
public interface InventoryRepository
  extends JpaRepository<InventoryEntity, String> {
}
