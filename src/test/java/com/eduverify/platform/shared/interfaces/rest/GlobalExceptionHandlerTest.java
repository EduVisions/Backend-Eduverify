package com.eduverify.platform.shared.interfaces.rest;

import com.eduverify.platform.shared.domain.exceptions.NotFoundException;
import com.eduverify.platform.shared.domain.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsIllegalArgumentExceptionTo400() {
        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(new IllegalArgumentException("bad input"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody().get("message"));
    }

    @Test
    void mapsNotFoundExceptionTo404() {
        ResponseEntity<Map<String, String>> response = handler.handleNotFound(new NotFoundException("missing"));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("missing", response.getBody().get("message"));
    }

    @Test
    void mapsUnauthorizedExceptionTo401() {
        ResponseEntity<Map<String, String>> response = handler.handleUnauthorized(new UnauthorizedException("no token"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("no token", response.getBody().get("message"));
    }
}
