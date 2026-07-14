package com.eduverify.platform.iam.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import com.eduverify.platform.shared.infrastructure.security.CurrentUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TokenAuthenticationFilterTest {
    private final TokenStore tokenStore = new TokenStore();
    private final TokenAuthenticationFilter filter = new TokenAuthenticationFilter(tokenStore);

    @Test
    void allowsExemptPathsWithoutToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/iam/login");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(response);
    }

    @Test
    void rejectsProtectedPathWithoutToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/exams");
        when(request.getHeader("Authorization")).thenReturn(null);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void rejectsProtectedPathWithUnknownToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/exams");
        when(request.getHeader("Authorization")).thenReturn("Bearer unknown-token");
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsProtectedPathWithValidTokenAndSetsCurrentUser() throws Exception {
        UserId userId = new UserId();
        String token = tokenStore.issue(userId);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getRequestURI()).thenReturn("/api/exams");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        doAnswer(invocation -> {
            assertEquals(userId, CurrentUserContext.get());
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNull(CurrentUserContext.get());
    }
}
