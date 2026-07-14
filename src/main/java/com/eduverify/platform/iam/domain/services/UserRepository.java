package com.eduverify.platform.iam.domain.services;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
