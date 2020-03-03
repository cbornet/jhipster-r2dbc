package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.ReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.query.Criteria;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public class UserRepository implements R2dbcRepository<User, Long> {
    private final DatabaseClient db;
    private final ReactiveDataAccessStrategy dataAccessStrategy;

    private final UserRepositoryInternal userRepository;

    public UserRepository(DatabaseClient db, ReactiveDataAccessStrategy dataAccessStrategy, UserRepositoryInternal userRepository) {
        this.db = db;
        this.dataAccessStrategy = dataAccessStrategy;
        this.userRepository = userRepository;
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

    //
    // Delegates
    //

    public Mono<User> findOneByActivationKey(String activationKey) {
        return userRepository.findOneByActivationKey(activationKey);
    }

    public Flux<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(OffsetDateTime dateTime) {
        return userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(dateTime);
    }

    public Mono<User> findOneByResetKey(String resetKey) {
        return userRepository.findOneByResetKey(resetKey);
    }

    public Mono<User> findOneByEmailIgnoreCase(String email) {
        return userRepository.findOneByEmailIgnoreCase(email);
    }

    public Mono<User> findOneByLogin(String login) {
        return userRepository.findOneByLogin(login);
    }

    public Mono<Long> countAllByLoginNot(String anonymousUser) {
        return userRepository.countAllByLoginNot(anonymousUser);
    }

    public Mono<Void> saveUserAuthority(Long userId, String authority) {
        return userRepository.saveUserAuthority(userId, authority);
    }

    public Mono<Void> deleteAllUserAuthorities() {
        return userRepository.deleteAllUserAuthorities();
    }

    public Mono<Void> deleteUserAuthoritiesByUserId(Long userId) {
        return userRepository.deleteUserAuthoritiesByUserId(userId);
    }

    @Override
    public <S extends User> Mono<S> save(S s) {
        return userRepository.save(s);
    }

    @Override
    public <S extends User> Flux<S> saveAll(Iterable<S> iterable) {
        return userRepository.saveAll(iterable);
    }

    @Override
    public <S extends User> Flux<S> saveAll(Publisher<S> publisher) {
        return userRepository.saveAll(publisher);
    }

    @Override
    public Mono<User> findById(Long aLong) {
        return userRepository.findById(aLong);
    }

    @Override
    public Mono<User> findById(Publisher<Long> publisher) {
        return userRepository.findById(publisher);
    }

    @Override
    public Mono<Boolean> existsById(Long aLong) {
        return userRepository.existsById(aLong);
    }

    @Override
    public Mono<Boolean> existsById(Publisher<Long> publisher) {
        return userRepository.existsById(publisher);
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Flux<User> findAllById(Iterable<Long> iterable) {
        return userRepository.findAllById(iterable);
    }

    @Override
    public Flux<User> findAllById(Publisher<Long> publisher) {
        return userRepository.findAllById(publisher);
    }

    @Override
    public Mono<Long> count() {
        return userRepository.count();
    }

    @Override
    public Mono<Void> deleteById(Long aLong) {
        return userRepository.deleteById(aLong);
    }

    @Override
    public Mono<Void> deleteById(Publisher<Long> publisher) {
        return userRepository.deleteById(publisher);
    }

    @Override
    public Mono<Void> delete(User user) {
        return userRepository.delete(user);
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends User> iterable) {
        return userRepository.deleteAll(iterable);
    }

    @Override
    public Mono<Void> deleteAll(Publisher<? extends User> publisher) {
        return userRepository.deleteAll(publisher);
    }

    @Override
    public Mono<Void> deleteAll() {
        return userRepository.deleteAll();
    }

}
