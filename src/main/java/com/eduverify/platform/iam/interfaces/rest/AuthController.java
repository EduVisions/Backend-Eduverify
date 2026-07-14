package com.eduverify.platform.iam.interfaces.rest;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import com.eduverify.platform.iam.domain.services.AuthenticatedUser;
import com.eduverify.platform.iam.domain.services.AuthenticationService;
import com.eduverify.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.eduverify.platform.iam.interfaces.rest.resources.LoginResource;
import com.eduverify.platform.iam.interfaces.rest.resources.RegisterResource;
import com.eduverify.platform.iam.interfaces.rest.resources.UserResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/iam")
public class AuthController {
    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResource> register(@RequestBody RegisterResource resource) {
        Role role = parseRole(resource.role());
        User user = authenticationService.register(resource.email(), resource.password(), role);
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResource(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticatedUserResource> login(@RequestBody LoginResource resource) {
        AuthenticatedUser authenticatedUser = authenticationService.login(resource.email(), resource.password());
        return ResponseEntity.ok(new AuthenticatedUserResource(
                authenticatedUser.token(),
                toUserResource(authenticatedUser.user())
        ));
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank())
            throw new IllegalArgumentException("Role cannot be null or blank");
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown role: " + role);
        }
    }

    private UserResource toUserResource(User user) {
        return new UserResource(user.getId().toString(), user.getEmail(), user.getRole().name());
    }
}
