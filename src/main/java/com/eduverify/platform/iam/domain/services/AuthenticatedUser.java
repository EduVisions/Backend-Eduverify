package com.eduverify.platform.iam.domain.services;

import com.eduverify.platform.iam.domain.model.aggregates.User;

public record AuthenticatedUser(User user, String token) {
}
