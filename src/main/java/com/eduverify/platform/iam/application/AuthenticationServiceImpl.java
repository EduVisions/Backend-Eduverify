package com.eduverify.platform.iam.application;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import com.eduverify.platform.iam.domain.services.AuthenticatedUser;
import com.eduverify.platform.iam.domain.services.AuthenticationService;
import com.eduverify.platform.iam.domain.services.UserRepository;
import com.eduverify.platform.iam.infrastructure.security.PasswordHasher;
import com.eduverify.platform.iam.infrastructure.security.TokenStore;
import com.eduverify.platform.shared.domain.exceptions.UnauthorizedException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenStore tokenStore;

    public AuthenticationServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher, TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenStore = tokenStore;
    }

    @Override
    public User register(String email, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email is already registered: " + email);

        User user = new User(email, passwordHasher.hash(rawPassword), role);
        return userRepository.save(user);
    }

    @Override
    public AuthenticatedUser login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordHasher.matches(rawPassword, user.getPasswordHash()))
            throw new UnauthorizedException("Invalid email or password");

        String token = tokenStore.issue(user.getId());
        return new AuthenticatedUser(user, token);
    }
}
