package org.example.authservice.filter;

import org.example.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private WebFilterChain webFilterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService);
    }

    // =========================================================
    // NO COOKIE TEST
    // =========================================================

    @Test
    void filter_ShouldContinueChain_WhenAccessTokenCookieIsMissing() {

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/users/me")
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(webFilterChain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        verify(webFilterChain).filter(exchange);

        verify(jwtService, never())
                .extractEmail(
                        org.mockito.ArgumentMatchers.anyString()
                );

        verify(jwtService, never())
                .extractRole(
                        org.mockito.ArgumentMatchers.anyString()
                );
    }

    // =========================================================
    // VALID BUYER TOKEN TEST
    // =========================================================

    @Test
    void filter_ShouldCreateBuyerAuthentication_WhenTokenIsValid() {

        String token = "valid-buyer-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/users/me")
                        .cookie(
                                new org.springframework.http.HttpCookie(
                                        "accessToken",
                                        token
                                )
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(token))
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("BUYER");

        AtomicReference<Authentication> capturedAuthentication =
                new AtomicReference<>();

        when(webFilterChain.filter(exchange))
                .thenReturn(
                        Mono.deferContextual(contextView ->
                                ReactiveSecurityContextHolder
                                        .getContext()
                                        .doOnNext(securityContext ->
                                                capturedAuthentication.set(
                                                        securityContext
                                                                .getAuthentication()
                                                )
                                        )
                                        .then()
                        )
                );

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        Authentication authentication =
                capturedAuthentication.get();

        assertNotNull(authentication);

        assertEquals(
                "buyer@gmail.com",
                authentication.getPrincipal()
        );

        assertTrue(authentication.isAuthenticated());

        assertEquals(
                1,
                authentication.getAuthorities().size()
        );

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_BUYER")
                        )
        );

        verify(jwtService).extractEmail(token);
        verify(jwtService).extractRole(token);
        verify(webFilterChain).filter(exchange);
    }

    // =========================================================
    // VALID ADMIN TOKEN TEST
    // =========================================================

    @Test
    void filter_ShouldCreateAdminAuthentication_WhenTokenIsValid() {

        String token = "valid-admin-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/admin/users")
                        .cookie(
                                new org.springframework.http.HttpCookie(
                                        "accessToken",
                                        token
                                )
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(token))
                .thenReturn("admin@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");

        AtomicReference<Authentication> capturedAuthentication =
                new AtomicReference<>();

        when(webFilterChain.filter(exchange))
                .thenReturn(
                        Mono.deferContextual(contextView ->
                                ReactiveSecurityContextHolder
                                        .getContext()
                                        .doOnNext(securityContext ->
                                                capturedAuthentication.set(
                                                        securityContext
                                                                .getAuthentication()
                                                )
                                        )
                                        .then()
                        )
                );

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        Authentication authentication =
                capturedAuthentication.get();

        assertNotNull(authentication);

        assertEquals(
                "admin@gmail.com",
                authentication.getPrincipal()
        );

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_ADMIN")
                        )
        );

        verify(jwtService).extractEmail(token);
        verify(jwtService).extractRole(token);
        verify(webFilterChain).filter(exchange);
    }

    // =========================================================
    // INVALID TOKEN TEST
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenTokenIsInvalid() {

        String invalidToken = "invalid-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/users/me")
                        .cookie(
                                new org.springframework.http.HttpCookie(
                                        "accessToken",
                                        invalidToken
                                )
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(invalidToken))
                .thenThrow(
                        new IllegalArgumentException(
                                "Invalid JWT"
                        )
                );

        when(webFilterChain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        verify(jwtService).extractEmail(invalidToken);

        verify(jwtService, never())
                .extractRole(invalidToken);

        verify(webFilterChain).filter(exchange);
    }

    // =========================================================
    // ROLE EXTRACTION FAILURE TEST
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenRoleExtractionFails() {

        String token = "token-with-invalid-role";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/users/me")
                        .cookie(
                                new org.springframework.http.HttpCookie(
                                        "accessToken",
                                        token
                                )
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(token))
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(token))
                .thenThrow(
                        new IllegalArgumentException(
                                "Role claim is missing"
                        )
                );

        when(webFilterChain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        verify(jwtService).extractEmail(token);
        verify(jwtService).extractRole(token);
        verify(webFilterChain).filter(exchange);
    }

    // =========================================================
    // EMPTY COOKIE TEST
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenCookieIsEmpty() {

        String emptyToken = "";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/users/me")
                        .cookie(
                                new org.springframework.http.HttpCookie(
                                        "accessToken",
                                        emptyToken
                                )
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(emptyToken))
                .thenThrow(
                        new IllegalArgumentException(
                                "JWT cannot be empty"
                        )
                );

        when(webFilterChain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        verify(jwtService).extractEmail(emptyToken);

        verify(jwtService, never())
                .extractRole(emptyToken);

        verify(webFilterChain).filter(exchange);
    }
}