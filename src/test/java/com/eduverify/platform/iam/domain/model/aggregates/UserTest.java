package com.eduverify.platform.iam.domain.model.aggregates;

import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {
    @Test
    void createsUserWithGeneratedId() {
        User user = new User("ana@universidad.edu", "hashed-value", Role.STUDENT);

        assertNotNull(user.getId());
        assertEquals("ana@universidad.edu", user.getEmail());
        assertEquals("hashed-value", user.getPasswordHash());
        assertEquals(Role.STUDENT, user.getRole());
    }

    @Test
    void rejectsBlankEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> new User("   ", "hashed-value", Role.STUDENT));
    }

    @Test
    void rejectsNullPasswordHash() {
        assertThrows(IllegalArgumentException.class,
                () -> new User("ana@universidad.edu", null, Role.STUDENT));
    }

    @Test
    void rejectsNullRole() {
        assertThrows(IllegalArgumentException.class,
                () -> new User("ana@universidad.edu", "hashed-value", null));
    }
}
