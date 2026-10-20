package edu.columbia.coms4156.ticketing.repository;

import edu.columbia.coms4156.ticketing.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Provides database access for events, including dynamic filtering and pagination. */
@Repository
public interface EventRepository
    extends JpaRepository<EventEntity, String>, JpaSpecificationExecutor<EventEntity> {}
