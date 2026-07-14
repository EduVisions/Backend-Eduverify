package com.eduverify.platform.iam.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void sameInputAlwaysProducesSameHash() {
        assertEquals(hasher.hash("Test1234!"), hasher.hash("Test1234!"));
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        assertNotEquals(hasher.hash("Test1234!"), hasher.hash("Different1!"));
    }

    @Test
    void matchesReturnsTrueForCorrectPassword() {
        String hashed = hasher.hash("Test1234!");
        assertTrue(hasher.matches("Test1234!", hashed));
    }

    @Test
    void matchesReturnsFalseForWrongPassword() {
        String hashed = hasher.hash("Test1234!");
        assertFalse(hasher.matches("wrong-password", hashed));
    }
}
