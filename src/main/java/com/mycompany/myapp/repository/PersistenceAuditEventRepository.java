package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.PersistentAuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Spring Data JPA repository for the {@link PersistentAuditEvent} entity.
 */
public interface PersistenceAuditEventRepository extends R2dbcRepository<PersistentAuditEvent, Long> {

    @Query("SELECT * FROM persistentAuditEvent WHERE auditEventDate BETWEEN :fromDate AND :toDate")
    Flux<PersistentAuditEvent> findAllByAuditEventDateBetween(Instant fromDate, Instant toDate, Pageable pageable);

    @Query("SELECT * FROM persistentAuditEvent WHERE auditEventDate BEFORE :before")
    Flux<PersistentAuditEvent> findByAuditEventDateBefore(Instant before);

    @Query("SELECT * FROM persistentAuditEvent")
    Flux<PersistentAuditEvent> findAllBy(Pageable pageable);

    @Query("COUNT(distinct id) FROM persistentAuditEvent WHERE auditEventDate BETWEEN :fromDate AND :toDate")
    Mono<Long> countByAuditEventDateBetween(Instant fromDate, Instant toDate);
}
