package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    @Query("SELECT * FROM jhi_user WHERE activation_key = :activationKey")
    Mono<User> findOneByActivationKey(String activationKey);

    @Query("SELECT * FROM jhi_user WHERE activated = false AND activation_key IS NOT NULL AND created_date < :dateTime")
    Flux<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(OffsetDateTime dateTime);

    @Query("SELECT * FROM jhi_user WHERE reset_key = :resetKey")
    Mono<User> findOneByResetKey(String resetKey);

    @Query("SELECT * FROM jhi_user WHERE LOWER(email) = LOWER(:email)")
    Mono<User> findOneByEmailIgnoreCase(String email);

    @Query("SELECT * FROM jhi_user WHERE login = :login")
    Mono<User> findOneByLogin(String login);

    @Query("SELECT COUNT(DISTINCT id) FROM jhi_user WHERE login != :anonymousUser")
    Mono<Long> countAllByLoginNot(String anonymousUser);

    @Query("INSERT INTO jhi_user_authority VALUES(:userId, :authority)")
    Mono<Void> saveUserAuthority(Long userId, String authority);

    @Query("DELETE FROM jhi_user_authority")
    Mono<Void> deleteAllUserAuthorities();

    @Query("DELETE FROM jhi_user_authority WHERE user_id = :userId")
    Mono<Void> deleteUserAuthoritiesByUserId(Long userId);



}
