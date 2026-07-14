package com.eduverify.platform.iam.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import com.eduverify.platform.shared.infrastructure.security.CurrentUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private static final Set<String> EXEMPT_PATHS = Set.of("/api/iam/register", "/api/iam/login");

    private final TokenStore tokenStore;

    public TokenAuthenticationFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (EXEMPT_PATHS.contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<UserId> userId = extractToken(request).flatMap(tokenStore::resolve);

        if (userId.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Missing or invalid authentication token\"}");
            return;
        }

        try {
            CurrentUserContext.set(userId.get());
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
        }
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            return Optional.empty();
        return Optional.of(header.substring("Bearer ".length()));
    }
}
