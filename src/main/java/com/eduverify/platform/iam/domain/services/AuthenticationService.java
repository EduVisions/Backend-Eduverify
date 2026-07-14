package com.eduverify.platform.iam.domain.services;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.model.valueobjects.Role;

public interface AuthenticationService {
    User register(String email, String rawPassword, Role role);
    AuthenticatedUser login(String email, String rawPassword);
}
