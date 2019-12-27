package com.mycompany.myapp.service;

import com.mycompany.myapp.config.Constants;
import com.mycompany.myapp.domain.Authority;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.AuthorityRepository;
import com.mycompany.myapp.repository.UserDatabaseClientRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.security.SecurityUtils;
import com.mycompany.myapp.service.dto.UserDTO;
import io.github.jhipster.security.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

/**
 * Service class for managing users.
 */
@Service
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthorityRepository authorityRepository;

    private final UserDatabaseClientRepository userDatabaseClientRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthorityRepository authorityRepository, UserDatabaseClientRepository userDatabaseClientRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorityRepository = authorityRepository;
        this.userDatabaseClientRepository = userDatabaseClientRepository;
    }

    @Transactional
    public Mono<User> activateRegistration(String key) {
        log.debug("Activating user for activation key {}", key);
        return userRepository.findOneByActivationKey(key)
            .flatMap(user -> {
                // activate given user for the registration key.
                user.setActivated(true);
                user.setActivationKey(null);
                return updateUser(user);
            })
            .doOnNext(user -> log.debug("Activated user: {}", user));
    }

    @Transactional
    public Mono<User> completePasswordReset(String newPassword, String key) {
        log.debug("Reset user password for reset key {}", key);
        return userRepository.findOneByResetKey(key)
            .filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400)))
            .flatMap(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetKey(null);
                user.setResetDate(null);
                return updateUser(user);
            });
    }

    @Transactional
    public Mono<User> requestPasswordReset(String mail) {
        return userRepository.findOneByEmailIgnoreCase(mail)
            .filter(User::getActivated)
            .flatMap(user -> {
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(Instant.now());
                return updateUser(user);
            });
    }

    @Transactional
    public Mono<User> registerUser(UserDTO userDTO, String password) {
        return userRepository.findOneByLogin(userDTO.getLogin().toLowerCase())
            .flatMap(existingUser -> {
                if (!existingUser.getActivated()) {
                    return deleteUser(existingUser);
                } else {
                    throw new UsernameAlreadyUsedException();
                }
            })
            .then(userRepository.findOneByEmailIgnoreCase(userDTO.getEmail()))
            .flatMap(existingUser -> {
                if (!existingUser.getActivated()) {
                    return deleteUser(existingUser);
                } else {
                    throw new EmailAlreadyUsedException();
                }
            })
            .thenReturn(new User())
            .flatMap(newUser -> {
                String encryptedPassword = passwordEncoder.encode(password);
                newUser.setLogin(userDTO.getLogin().toLowerCase());
                // new user gets initially a generated password
                newUser.setPassword(encryptedPassword);
                newUser.setFirstName(userDTO.getFirstName());
                newUser.setLastName(userDTO.getLastName());
                if (userDTO.getEmail() != null) {
                    newUser.setEmail(userDTO.getEmail().toLowerCase());
                }
                newUser.setImageUrl(userDTO.getImageUrl());
                newUser.setLangKey(userDTO.getLangKey());
                // new user is not active
                newUser.setActivated(false);
                // new user gets registration key
                newUser.setActivationKey(RandomUtil.generateActivationKey());
                Set<Authority> authorities = new HashSet<>();
                return authorityRepository.findById(AuthoritiesConstants.USER)
                    .map(authorities::add)
                    .thenReturn(newUser)
                    .doOnNext(user -> user.setAuthorities(authorities))
                    .flatMap(this::createUser)
                    .doOnNext(user -> log.debug("Created Information for User: {}", user));
            });
    }

    @Transactional
    public Mono<User> createUser(UserDTO userDTO) {
        User user = new User();
        user.setLogin(userDTO.getLogin().toLowerCase());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail().toLowerCase());
        }
        user.setImageUrl(userDTO.getImageUrl());
        if (userDTO.getLangKey() == null) {
            user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
        } else {
            user.setLangKey(userDTO.getLangKey());
        }
        String encryptedPassword = passwordEncoder.encode(RandomUtil.generatePassword());
        user.setPassword(encryptedPassword);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        user.setActivated(true);
        return Flux.fromIterable(userDTO.getAuthorities() != null ? userDTO.getAuthorities() : new HashSet<>())
            .flatMap(authorityRepository::findById)
            .doOnNext(authority -> user.getAuthorities().add(authority))
            .then(createUser(user))
            .doOnNext(user1 -> log.debug("Created Information for User: {}", user1));
    }

    /**
     * Update basic information (first name, last name, email, language) for the current user.
     *
     * @param firstName first name of user.
     * @param lastName  last name of user.
     * @param email     email id of user.
     * @param langKey   language key.
     * @param imageUrl  image URL of user.
     */
    @Transactional
    public Mono<Void> updateUser(String firstName, String lastName, String email, String langKey, String imageUrl) {
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .flatMap(user -> {
                user.setFirstName(firstName);
                user.setLastName(lastName);
                if (email != null) {
	                user.setEmail(email.toLowerCase());
                }
                user.setLangKey(langKey);
                user.setImageUrl(imageUrl);
                return updateUser(user);
            })
            .doOnNext(user -> log.debug("Changed Information for User: {}", user))
            .then();
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO user to update.
     * @return updated user.
     */
    @Transactional
    public Mono<UserDTO> updateUser(UserDTO userDTO) {
        return userRepository.findById(userDTO.getId())
            .flatMap(user -> {
                user.setLogin(userDTO.getLogin().toLowerCase());
                user.setFirstName(userDTO.getFirstName());
                user.setLastName(userDTO.getLastName());
                if (userDTO.getEmail() != null) {
                    user.setEmail(userDTO.getEmail().toLowerCase());
                }
                user.setImageUrl(userDTO.getImageUrl());
                user.setActivated(userDTO.isActivated());
                user.setLangKey(userDTO.getLangKey());
                Set<Authority> managedAuthorities = user.getAuthorities();
                managedAuthorities.clear();
                return Flux.fromIterable(userDTO.getAuthorities())
                    .flatMap(authorityRepository::findById)
                    .map(managedAuthorities::add)
                    .then(Mono.just(user));
            })
            .flatMap(this::updateUser)
            .doOnNext(user -> log.debug("Changed Information for User: {}", user))
            .map(UserDTO::new);
    }

    @Transactional
    private Mono<User> updateUser(User user) {
        return SecurityUtils.getCurrentUserLogin()
            .switchIfEmpty(Mono.just(Constants.SYSTEM_ACCOUNT))
            .flatMap(login -> {
                user.setLastModifiedBy(login);
                return userRepository.save(user);
            });
    }

    @Transactional
    private Mono<User> createUser(User user) {
        return SecurityUtils.getCurrentUserLogin()
            .switchIfEmpty(Mono.just(Constants.SYSTEM_ACCOUNT))
            .flatMap(login -> {
                user.setCreatedBy(login);
                user.setLastModifiedBy(login);
                return userRepository.save(user)
                    .flatMap(savedUser ->
                       Flux.fromIterable(user.getAuthorities())
                           .flatMap(authority -> userRepository.saveUserAuthority(savedUser.getId(), authority.getName()))
                           .then(Mono.just(savedUser))
                    );
            });
    }

    @Transactional
    public Mono<Void> deleteUser(String login) {
        return userRepository.findOneByLogin(login)
            .flatMap(user ->deleteUser(user).thenReturn(user))
            .doOnNext(user -> log.debug("Deleted User: {}", user))
            .then();
    }

    @Transactional
    public Mono<Void> deleteUser(User user) {
        if (user.getId() == null) {
            return Mono.empty();
        }
        return userRepository.deleteUserAuthoritiesByUserId(user.getId())
            .then(userRepository.delete(user));
    }

    @Transactional
    public Mono<Void> changePassword(String currentClearTextPassword, String newPassword) {
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .flatMap(user -> {
                String currentEncryptedPassword = user.getPassword();
                if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                    throw new InvalidPasswordException();
                }
                String encryptedPassword = passwordEncoder.encode(newPassword);
                user.setPassword(encryptedPassword);
                return updateUser(user);
            })
            .doOnNext(user -> log.debug("Changed password for User: {}", user))
            .then();
    }

    @Transactional(readOnly = true)
    public Flux<UserDTO> getAllManagedUsers(Pageable pageable) {
        return userDatabaseClientRepository.findAllByLoginNot(pageable, Constants.ANONYMOUS_USER).map(UserDTO::new);
    }

    public Mono<Long> countManagedUsers() {
        return userRepository.countAllByLoginNot(Constants.ANONYMOUS_USER);
    }

    @Transactional(readOnly = true)
    public Mono<User> getUserWithAuthoritiesByLogin(String login) {
        return userDatabaseClientRepository.findOneWithAuthoritiesByLogin(login);
    }

    @Transactional(readOnly = true)
    public Mono<User> getUserWithAuthorities(Long id) {
        return userDatabaseClientRepository.findOneWithAuthoritiesById(id);
    }

    @Transactional(readOnly = true)
    public Mono<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userDatabaseClientRepository::findOneWithAuthoritiesByLogin);
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        removeNotActivatedUsersReactively()
            .subscribeOn(Schedulers.elastic())
            .blockLast();
    }

    @Transactional
    public Flux<User> removeNotActivatedUsersReactively() {
        return userRepository
            .findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(OffsetDateTime.now().minus(3, ChronoUnit.DAYS))
            .flatMap(user -> deleteUser(user).thenReturn(user))
            .doOnNext(user -> log.debug("Deleted User: {}", user));
    }

    /**
     * Gets a list of all the authorities.
     * @return a list of all the authorities.
     */
    @Transactional
    public Flux<String> getAuthorities() {
        return authorityRepository.findAll().map(Authority::getName);
    }

}
