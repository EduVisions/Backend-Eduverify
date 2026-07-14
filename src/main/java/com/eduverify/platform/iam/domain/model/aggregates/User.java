package com.eduverify.platform.iam.domain.model.aggregates;

import com.eduverify.platform.iam.domain.model.valueobjects.Role;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Objects;

@Getter
public class User {
    private final UserId id;
    @Setter @NonNull private String email;
    @Setter @NonNull private String passwordHash;
    private final Role role;

    public User(String email, String passwordHash, Role role) {
        if (Objects.isNull(email) || email.isBlank())
            throw new IllegalArgumentException("User email cannot be null or blank");
        if (Objects.isNull(passwordHash) || passwordHash.isBlank())
            throw new IllegalArgumentException("User password hash cannot be null or blank");
        if (Objects.isNull(role))
            throw new IllegalArgumentException("User role cannot be null");

        this.id = new UserId();
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}
