package org.example.verifyservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
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
import static org.mockito.ArgumentMatchers.anyString;
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
    // MISSING TOKEN
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenTokenIsMissing() {

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
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

        verify(webFilterChain)
                .filter(exchange);

        verify(jwtService, never())
                .extractEmail(anyString());

        verify(jwtService, never())
                .extractRole(anyString());

        verify(jwtService, never())
                .extractUserId(anyString());
    }

    // =========================================================
    // ADMIN COOKIE TOKEN
    // =========================================================

    @Test
    void filter_ShouldCreateAdminAuthentication_FromCookieToken() {

        String token = "valid-admin-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .cookie(
                                new HttpCookie(
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

        when(jwtService.extractUserId(token))
                .thenReturn(1);

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
                authentication.isAuthenticated()
        );

        assertEquals(
                1,
                authentication.getAuthorities().size()
        );

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_ADMIN")
                        )
        );

        verify(jwtService)
                .extractEmail(token);

        verify(jwtService)
                .extractRole(token);

        verify(jwtService)
                .extractUserId(token);

        verify(webFilterChain)
                .filter(exchange);
    }

    // =========================================================
    // BUYER COOKIE TOKEN
    // =========================================================

    @Test
    void filter_ShouldCreateBuyerAuthentication_FromCookieToken() {

        String token = "valid-buyer-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get(
                                "/api/v1/verifications/applications/application-101"
                        )
                        .cookie(
                                new HttpCookie(
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

        when(jwtService.extractUserId(token))
                .thenReturn(4);

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

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_BUYER")
                        )
        );

        verify(jwtService)
                .extractEmail(token);

        verify(jwtService)
                .extractRole(token);

        verify(jwtService)
                .extractUserId(token);
    }

    // =========================================================
    // BEARER HEADER
    // =========================================================

    @Test
    void filter_ShouldCreateAuthentication_FromBearerHeader() {

        String token = "valid-admin-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(token))
                .thenReturn("admin@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(token))
                .thenReturn(1);

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

        verify(jwtService)
                .extractEmail(token);

        verify(jwtService)
                .extractRole(token);

        verify(jwtService)
                .extractUserId(token);
    }

    // =========================================================
    // RAW AUTHORIZATION HEADER
    // =========================================================

    @Test
    void filter_ShouldCreateAuthentication_FromRawAuthorizationHeader() {

        String token = "raw-admin-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                token
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(token))
                .thenReturn("admin@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(token))
                .thenReturn(1);

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

        verify(jwtService)
                .extractEmail(token);

        verify(jwtService)
                .extractRole(token);

        verify(jwtService)
                .extractUserId(token);
    }

    // =========================================================
    // ROLE_ PREFIX
    // =========================================================

    @Test
    void filter_ShouldNotDuplicateRolePrefix_WhenRoleAlreadyHasPrefix() {

        String token = "prefixed-role-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .cookie(
                                new HttpCookie(
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
                .thenReturn("ROLE_ADMIN");

        when(jwtService.extractUserId(token))
                .thenReturn(1);

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

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_ADMIN")
                        )
        );

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .noneMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_ROLE_ADMIN")
                        )
        );
    }

    // =========================================================
    // COOKIE PRIORITY
    // =========================================================

    @Test
    void filter_ShouldPreferCookieToken_OverAuthorizationHeader() {

        String cookieToken = "cookie-admin-token";
        String headerToken = "header-buyer-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .cookie(
                                new HttpCookie(
                                        "accessToken",
                                        cookieToken
                                )
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + headerToken
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(cookieToken))
                .thenReturn("admin@gmail.com");

        when(jwtService.extractRole(cookieToken))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(cookieToken))
                .thenReturn(1);

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

        verify(jwtService)
                .extractEmail(cookieToken);

        verify(jwtService)
                .extractRole(cookieToken);

        verify(jwtService)
                .extractUserId(cookieToken);

        verify(jwtService, never())
                .extractEmail(headerToken);

        verify(jwtService, never())
                .extractRole(headerToken);

        verify(jwtService, never())
                .extractUserId(headerToken);
    }

    // =========================================================
    // INVALID TOKEN
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenTokenIsInvalid() {

        String token = "invalid-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .cookie(
                                new HttpCookie(
                                        "accessToken",
                                        token
                                )
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(token))
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

        verify(jwtService)
                .extractEmail(token);

        verify(jwtService, never())
                .extractRole(token);

        verify(jwtService, never())
                .extractUserId(token);

        verify(webFilterChain)
                .filter(exchange);
    }

    // =========================================================
    // MISSING EMAIL CLAIM
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenEmailIsMissing() {

        String token = "token-without-email";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .cookie(
                                new HttpCookie(
                                        "accessToken",
                                        token
                                )
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(token))
                .thenReturn(null);

        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(token))
                .thenReturn(1);

        when(webFilterChain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        verify(jwtService)
                .extractEmail(token);

        verify(jwtService)
                .extractRole(token);

        verify(jwtService)
                .extractUserId(token);

        verify(webFilterChain)
                .filter(exchange);
    }

    // =========================================================
    // BLANK EMAIL CLAIM
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenEmailIsBlank() {

        String token = "token-with-blank-email";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .cookie(
                                new HttpCookie(
                                        "accessToken",
                                        token
                                )
                        )
                        .build();

        MockServerWebExchange exchange =
                MockServerWebExchange.from(request);

        when(jwtService.extractEmail(token))
                .thenReturn("   ");

        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(token))
                .thenReturn(1);

        when(webFilterChain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        verify(webFilterChain)
                .filter(exchange);
    }

    // =========================================================
    // MISSING ROLE CLAIM
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenRoleIsMissing() {

        String token = "token-without-role";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .cookie(
                                new HttpCookie(
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
                .thenReturn(null);

        when(jwtService.extractUserId(token))
                .thenReturn(1);

        when(webFilterChain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        verify(webFilterChain)
                .filter(exchange);
    }

    // =========================================================
    // BLANK ROLE CLAIM
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenRoleIsBlank() {

        String token = "token-with-blank-role";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .cookie(
                                new HttpCookie(
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
                .thenReturn("   ");

        when(jwtService.extractUserId(token))
                .thenReturn(1);

        when(webFilterChain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        verify(webFilterChain)
                .filter(exchange);
    }

    // =========================================================
    // MISSING USER ID CLAIM
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenUserIdIsMissing() {

        String token = "token-without-user-id";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .cookie(
                                new HttpCookie(
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

        when(jwtService.extractUserId(token))
                .thenReturn(null);

        when(webFilterChain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                        jwtAuthenticationFilter.filter(
                                exchange,
                                webFilterChain
                        )
                )
                .verifyComplete();

        verify(webFilterChain)
                .filter(exchange);
    }

    // =========================================================
    // EMPTY COOKIE
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenCookieIsEmpty() {

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get("/api/v1/verifications/applications")
                        .cookie(
                                new HttpCookie(
                                        "accessToken",
                                        ""
                                )
                        )
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

        verify(jwtService, never())
                .extractEmail(anyString());

        verify(jwtService, never())
                .extractRole(anyString());

        verify(jwtService, never())
                .extractUserId(anyString());

        verify(webFilterChain)
                .filter(exchange);
    }
}