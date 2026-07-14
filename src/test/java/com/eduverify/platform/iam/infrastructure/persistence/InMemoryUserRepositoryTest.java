package com.eduverify.platform.iam.infrastructure.persistence;

import com.eduverify.platform.iam.domain.model.aggregates.User;
import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryUserRepositoryTest {
    private final InMemoryUserRepository repository = new InMemoryUserRepository();

    @Test
    void savesAndFindsUserById() {
        User user = new User("ana@universidad.edu", "hash", Role.STUDENT);
        repository.save(user);

        assertTrue(repository.findById(user.getId()).isPresent());
        assertEquals(user.getEmail(), repository.findById(user.getId()).get().getEmail());
    }

    @Test
    void findsUserByEmailCaseInsensitive() {
        User user = new User("Ana@Universidad.edu", "hash", Role.STUDENT);
        repository.save(user);

        assertTrue(repository.findByEmail("ana@universidad.edu").isPresent());
    }

    @Test
    void existsByEmailReflectsSavedUsers() {
        assertFalse(repository.existsByEmail("ana@universidad.edu"));

        repository.save(new User("ana@universidad.edu", "hash", Role.STUDENT));

        assertTrue(repository.existsByEmail("ana@universidad.edu"));
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertTrue(repository.findById(new UserId()).isEmpty());
    }
}
