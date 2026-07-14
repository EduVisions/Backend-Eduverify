package com.eduverify.platform.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(String token, UserResource user) {
}
