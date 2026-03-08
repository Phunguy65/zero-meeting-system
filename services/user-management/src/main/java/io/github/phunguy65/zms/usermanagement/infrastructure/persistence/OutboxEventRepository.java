package io.github.phunguy65.zms.usermanagement.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link OutboxEventEntity}. */
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /** Returns all outbox rows that have not yet been published to Kafka. */
    List<OutboxEventEntity> findAllByPublishedAtIsNull();
}
