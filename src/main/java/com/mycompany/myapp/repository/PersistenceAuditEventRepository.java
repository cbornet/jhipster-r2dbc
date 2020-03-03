package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.PersistentAuditEvent;
import io.r2dbc.h2.codecs.DefaultCodecs;
import org.reactivestreams.Publisher;
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
import java.time.ZoneOffset;

/**
 * Spring Data JPA repository for the {@link PersistentAuditEvent} entity.
 */
@Repository
public class PersistenceAuditEventRepository implements R2dbcRepository<PersistentAuditEvent, Long> {

    private final DatabaseClient databaseClient;
    private final PersistenceAuditEventRepositoryInternal persistenceAuditEventRepository;

    public PersistenceAuditEventRepository(DatabaseClient databaseClient, PersistenceAuditEventRepositoryInternal persistenceAuditEventRepository) {
        this.persistenceAuditEventRepository = persistenceAuditEventRepository;
        this.databaseClient = databaseClient;
    }

    public Flux<PersistentAuditEvent> findByPrincipal(String principal) {
        return findAllByCriteria(Criteria.where("principal").is(principal));
    }

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

    public Flux<PersistentAuditEvent> findByAuditEventDateBefore(OffsetDateTime before) {
        return findAllByCriteria(Criteria.where("event_date").lessThan(before));
    }

    public Flux<PersistentAuditEvent> findAllBy(Pageable pageable) {
        return findAllFromSpec(select().page(pageable));
    }

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

    @Override
    public <S extends PersistentAuditEvent> Mono<S> save(S s) {
        return persistenceAuditEventRepository.save(s);
    }

    @Override
    public <S extends PersistentAuditEvent> Flux<S> saveAll(Iterable<S> iterable) {
        return persistenceAuditEventRepository.saveAll(iterable);
    }

    @Override
    public <S extends PersistentAuditEvent> Flux<S> saveAll(Publisher<S> publisher) {
        return persistenceAuditEventRepository.saveAll(publisher);
    }

    @Override
    public Mono<PersistentAuditEvent> findById(Long aLong) {
        return persistenceAuditEventRepository.findById(aLong);
    }

    @Override
    public Mono<PersistentAuditEvent> findById(Publisher<Long> publisher) {
        return persistenceAuditEventRepository.findById(publisher);
    }

    @Override
    public Mono<Boolean> existsById(Long aLong) {
        return persistenceAuditEventRepository.existsById(aLong);
    }

    @Override
    public Mono<Boolean> existsById(Publisher<Long> publisher) {
        return persistenceAuditEventRepository.existsById(publisher);
    }

    @Override
    public Flux<PersistentAuditEvent> findAll() {
        return persistenceAuditEventRepository.findAll();
    }

    @Override
    public Flux<PersistentAuditEvent> findAllById(Iterable<Long> iterable) {
        return persistenceAuditEventRepository.findAllById(iterable);
    }

    @Override
    public Flux<PersistentAuditEvent> findAllById(Publisher<Long> publisher) {
        return persistenceAuditEventRepository.findAllById(publisher);
    }

    @Override
    public Mono<Long> count() {
        return persistenceAuditEventRepository.count();
    }

    @Override
    public Mono<Void> deleteById(Long aLong) {
        return persistenceAuditEventRepository.deleteById(aLong);
    }

    @Override
    public Mono<Void> deleteById(Publisher<Long> publisher) {
        return persistenceAuditEventRepository.deleteById(publisher);
    }

    @Override
    public Mono<Void> delete(PersistentAuditEvent persistentAuditEvent) {
        return persistenceAuditEventRepository.delete(persistentAuditEvent);
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends PersistentAuditEvent> iterable) {
        return persistenceAuditEventRepository.deleteAll(iterable);
    }

    @Override
    public Mono<Void> deleteAll(Publisher<? extends PersistentAuditEvent> publisher) {
        return persistenceAuditEventRepository.deleteAll(publisher);
    }

    @Override
    public Mono<Void> deleteAll() {
        return persistenceAuditEventRepository.deleteAll();
    }
}
