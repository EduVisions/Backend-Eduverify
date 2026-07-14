package com.eduverify.platform.iam.application;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import com.eduverify.platform.iam.domain.services.AuthenticatedUser;
import com.eduverify.platform.iam.infrastructure.persistence.InMemoryUserRepository;
import com.eduverify.platform.iam.infrastructure.security.PasswordHasher;
import com.eduverify.platform.iam.infrastructure.security.TokenStore;
import com.eduverify.platform.shared.domain.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceImplTest {
    private AuthenticationServiceImpl service;
    private InMemoryUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        service = new AuthenticationServiceImpl(userRepository, new PasswordHasher(), new TokenStore());
    }

    @Test
    void registersNewUser() {
        User user = service.register("ana@universidad.edu", "Test1234!", Role.STUDENT);

        assertNotNull(user.getId());
        assertTrue(userRepository.existsByEmail("ana@universidad.edu"));
    }

    @Test
    void rejectsRegisteringDuplicateEmail() {
        service.register("ana@universidad.edu", "Test1234!", Role.STUDENT);

        assertThrows(IllegalArgumentException.class,
                () -> service.register("ana@universidad.edu", "Other123!", Role.TEACHER));
    }

    @Test
    void logsInWithCorrectCredentials() {
        service.register("ana@universidad.edu", "Test1234!", Role.STUDENT);

        AuthenticatedUser authenticatedUser = service.login("ana@universidad.edu", "Test1234!");

        assertNotNull(authenticatedUser.token());
        assertEquals("ana@universidad.edu", authenticatedUser.user().getEmail());
    }

    @Test
    void rejectsLoginWithWrongPassword() {
        service.register("ana@universidad.edu", "Test1234!", Role.STUDENT);

        assertThrows(UnauthorizedException.class,
                () -> service.login("ana@universidad.edu", "wrong-password"));
    }

    @Test
    void rejectsLoginForUnknownEmail() {
        assertThrows(UnauthorizedException.class,
                () -> service.login("unknown@universidad.edu", "Test1234!"));
    }
}
