package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.R2DbcApp;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.web.rest.vm.LoginVM;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for the {@link UserJWTController} REST controller.
 */
@AutoConfigureWebTestClient
@SpringBootTest(classes = R2DbcApp.class)
public class UserJWTControllerIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @Transactional
    public void testAuthorize() throws Exception {
        User user = new User();
        user.setLogin("user-jwt-controller");
        user.setEmail("user-jwt-controller@example.com");
        user.setActivated(true);
        user.setPassword(passwordEncoder.encode("test"));

        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-jwt-controller");
        login.setPassword("test");
        webTestClient.post().uri("/api/authenticate")
            .contentType(TestUtil.APPLICATION_JSON)
            .syncBody(TestUtil.convertObjectToJsonBytes(login))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueMatches("Authorization", "Bearer .+")
            .expectBody()
            .jsonPath("$.id_token").isNotEmpty();
    }

    @Test
    @Transactional
    public void testAuthorizeWithRememberMe() throws Exception {
        User user = new User();
        user.setLogin("user-jwt-controller-remember-me");
        user.setEmail("user-jwt-controller-remember-me@example.com");
        user.setActivated(true);
        user.setPassword(passwordEncoder.encode("test"));

        userRepository.saveAndFlush(user);

        LoginVM login = new LoginVM();
        login.setUsername("user-jwt-controller-remember-me");
        login.setPassword("test");
        login.setRememberMe(true);
        webTestClient.post().uri("/api/authenticate")
            .contentType(TestUtil.APPLICATION_JSON)
            .syncBody(TestUtil.convertObjectToJsonBytes(login))
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueMatches("Authorization", "Bearer .+")
            .expectBody()
            .jsonPath("$.id_token").isNotEmpty();
    }

    @Test
    public void testAuthorizeFails() throws Exception {
        LoginVM login = new LoginVM();
        login.setUsername("wrong-user");
        login.setPassword("wrong password");
        webTestClient.post().uri("/api/authenticate")
            .contentType(TestUtil.APPLICATION_JSON)
            .syncBody(TestUtil.convertObjectToJsonBytes(login))
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().doesNotExist("Authorization")
            .expectBody()
            .jsonPath("$.id_token").doesNotExist();
    }
}
