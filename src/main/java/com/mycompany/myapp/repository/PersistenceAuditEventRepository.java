package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.PersistentAuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.query.Criteria;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Spring Data JPA repository for the {@link PersistentAuditEvent} entity.
 */
@Repository
public interface PersistenceAuditEventRepository extends R2dbcRepository<PersistentAuditEvent, Long>, PersistenceAuditEventRepositoryInternal {
}

interface PersistenceAuditEventRepositoryInternal {

    Flux<PersistentAuditEvent> findByPrincipal(String principal);

    Flux<PersistentAuditEvent> findAllByAuditEventDateBetween(Instant fromDate, Instant toDate, Pageable pageable);

    Flux<PersistentAuditEvent> findByAuditEventDateBefore(OffsetDateTime before);

    Flux<PersistentAuditEvent> findAllBy(Pageable pageable);

    Mono<Long> countByAuditEventDateBetween(Instant fromDate, Instant toDate);
}

class PersistenceAuditEventRepositoryInternalImpl implements PersistenceAuditEventRepositoryInternal {

    private final DatabaseClient databaseClient;

    public PersistenceAuditEventRepositoryInternalImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<PersistentAuditEvent> findByPrincipal(String principal) {
        return findAllByCriteria(Criteria.where("principal").is(principal));
    }

    @Override
    public Flux<PersistentAuditEvent> findAllByAuditEventDateBetween(Instant fromDate, Instant toDate, Pageable pageable) {
        // Can be removed in 0.8.3+ version of r2dbc-h2
        // See https://github.com/r2dbc/r2dbc-h2/pull/139
        OffsetDateTime fromDateH2 = OffsetDateTime.ofInstant(fromDate, ZoneId.systemDefault());
        OffsetDateTime toDateH2 = OffsetDateTime.ofInstant(toDate, ZoneId.systemDefault());
        Criteria criteria = Criteria
            .where("event_date").greaterThan(fromDateH2)
            .and("event_date").lessThan(toDateH2);
        return findAllFromSpec(select().matching(criteria).page(pageable));
    }

    @Override
    public Flux<PersistentAuditEvent> findByAuditEventDateBefore(OffsetDateTime before) {
        return findAllByCriteria(Criteria.where("event_date").lessThan(before));
    }

    @Override
    public Flux<PersistentAuditEvent> findAllBy(Pageable pageable) {
        return findAllFromSpec(select().page(pageable));
    }

    @Override
    public Mono<Long> countByAuditEventDateBetween(Instant fromDate, Instant toDate) {
        // Can be removed in 0.8.3+ version of r2dbc-h2
        // See https://github.com/r2dbc/r2dbc-h2/pull/139
        OffsetDateTime fromDateH2 = OffsetDateTime.ofInstant(fromDate, ZoneId.systemDefault());
        OffsetDateTime toDateH2 = OffsetDateTime.ofInstant(toDate, ZoneId.systemDefault());
        return databaseClient.execute("SELECT COUNT(DISTINCT event_id) FROM jhi_persistent_audit_event " +
            "WHERE event_date > :fromDate AND event_date < :toDate")
            .bind("fromDate", fromDateH2)
            .bind("toDate", toDateH2)
            .as(Long.class)
            .fetch()
            .one();
    }

    private Flux<PersistentAuditEvent> findAllByCriteria(Criteria criteria) {
        return findAllFromSpec(select().matching(criteria));
    }

    private DatabaseClient.TypedSelectSpec<PersistentAuditEvent> select() {
        return databaseClient.select().from(PersistentAuditEvent.class);
    }

    private Flux<PersistentAuditEvent> findAllFromSpec(DatabaseClient.TypedSelectSpec<PersistentAuditEvent> spec) {
        return spec.as(PersistentAuditEvent.class).all();
    }
}



