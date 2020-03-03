package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.PersistentAuditEvent;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.query.Criteria;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.channels.MembershipKey;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
interface PersistenceAuditEventRepositoryInternal extends R2dbcRepository<PersistentAuditEvent, Long> {
}
