package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class UserDatabaseClientRepository {
    private final DatabaseClient db;
    private final ReactiveDataAccessStrategy dataAccessStrategy;


    public UserDatabaseClientRepository(DatabaseClient db, ReactiveDataAccessStrategy dataAccessStrategy) {
        this.db = db;
        this.dataAccessStrategy = dataAccessStrategy;
    }

    public Mono<User> findOneWithAuthoritiesByLogin(String login) {
        return findOneWithAuthoritiesBy("login", login);
    }

    public Mono<User> findOneWithAuthoritiesById(Long id) {
        return findOneWithAuthoritiesBy("id", id);
    }

    public Mono<User> findOneWithAuthoritiesByEmailIgnoreCase(String email) {
        return findOneWithAuthoritiesBy("email", email.toLowerCase());
    }

    public Mono<User> findOneWithAuthoritiesBy(String fieldName, Object fieldValue) {
        return db.execute("SELECT * FROM jhi_user u LEFT JOIN jhi_user_authority ua ON u.id=ua.user_id WHERE u." + fieldName + " = :" + fieldName)
            .bind(fieldName, fieldValue)
            .map((row, metadata) ->
                Tuples.of(
                    dataAccessStrategy.getRowMapper(User.class).apply(row, metadata),
                    Optional.ofNullable(row.get("authority_name", String.class))
                )
            )
            .all()
            .collectList()
            .filter(l -> !l.isEmpty())
            .map(l -> {
                User user = l.get(0).getT1();
                user.setAuthorities(
                    l.stream()
                        .filter(t -> t.getT2().isPresent())
                        .map(t -> {
                            Authority authority = new Authority();
                            authority.setName(t.getT2().get());
                            return authority;
                        })
                        .collect(Collectors.toSet())
                );
                return user;
            });
    }

    public Flux<User> findAllByLoginNot(Pageable pageable, String login) {
        return db.select().from(User.class)
            .matching(Criteria.where("login").not(login))
            .page(pageable)
            .as(User.class)
            .all();
    }
}
