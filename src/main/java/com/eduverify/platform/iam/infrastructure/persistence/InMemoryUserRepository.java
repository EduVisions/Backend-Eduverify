package com.eduverify.platform.iam.infrastructure.persistence;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.services.UserRepository;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {
    private final Map<UserId, User> usersById = new ConcurrentHashMap<>();

    @Override
    public User save(User user) {
        usersById.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return usersById.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }
}
