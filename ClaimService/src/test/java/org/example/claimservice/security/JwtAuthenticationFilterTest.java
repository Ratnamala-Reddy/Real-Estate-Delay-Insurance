package org.example.claimservice.security;

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
                new JwtAuthenticationFilter(
                        jwtService
                );
    }

    // =========================================================
    // MISSING TOKEN
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenTokenIsMissing() {

        MockServerWebExchange exchange =
                createExchangeWithoutToken();

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
    // BUYER COOKIE
    // =========================================================

    @Test
    void filter_ShouldCreateBuyerAuthentication_FromCookieToken() {

        String token =
                "valid-buyer-token";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

        when(jwtService.extractEmail(token))
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("BUYER");

        when(jwtService.extractUserId(token))
                .thenReturn(4);

        AtomicReference<Authentication> capturedAuthentication =
                captureAuthentication(exchange);

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
                authentication.isAuthenticated()
        );

        assertTrue(
                authentication.getPrincipal()
                        instanceof AuthenticatedUser
        );

        AuthenticatedUser authenticatedUser =
                (AuthenticatedUser)
                        authentication.getPrincipal();

        assertEquals(
                4,
                authenticatedUser.userId()
        );

        assertEquals(
                "buyer@gmail.com",
                authenticatedUser.email()
        );

        assertEquals(
                "BUYER",
                authenticatedUser.role()
        );

        assertTrue(
                containsAuthority(
                        authentication,
                        "ROLE_BUYER"
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
    // ADMIN COOKIE
    // =========================================================

    @Test
    void filter_ShouldCreateAdminAuthentication_FromCookieToken() {

        String token =
                "valid-admin-token";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

        when(jwtService.extractEmail(token))
                .thenReturn("admin@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(token))
                .thenReturn(1);

        AtomicReference<Authentication> capturedAuthentication =
                captureAuthentication(exchange);

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

        AuthenticatedUser authenticatedUser =
                (AuthenticatedUser)
                        authentication.getPrincipal();

        assertEquals(
                1,
                authenticatedUser.userId()
        );

        assertEquals(
                "admin@gmail.com",
                authenticatedUser.email()
        );

        assertEquals(
                "ADMIN",
                authenticatedUser.role()
        );

        assertTrue(
                containsAuthority(
                        authentication,
                        "ROLE_ADMIN"
                )
        );
    }

    // =========================================================
    // BEARER TOKEN
    // =========================================================

    @Test
    void filter_ShouldCreateAuthentication_FromBearerHeader() {

        String token =
                "valid-service-token";

        MockServerWebExchange exchange =
                createExchangeWithAuthorizationHeader(
                        "Bearer " + token
                );

        when(jwtService.extractEmail(token))
                .thenReturn("claim-service");

        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(token))
                .thenReturn(0);

        AtomicReference<Authentication> capturedAuthentication =
                captureAuthentication(exchange);

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

        AuthenticatedUser authenticatedUser =
                (AuthenticatedUser)
                        authentication.getPrincipal();

        assertEquals(
                0,
                authenticatedUser.userId()
        );

        assertEquals(
                "claim-service",
                authenticatedUser.email()
        );

        assertEquals(
                "ADMIN",
                authenticatedUser.role()
        );

        assertTrue(
                containsAuthority(
                        authentication,
                        "ROLE_ADMIN"
                )
        );
    }

    // =========================================================
    // RAW AUTHORIZATION HEADER
    // =========================================================

    @Test
    void filter_ShouldCreateAuthentication_FromRawAuthorizationHeader() {

        String token =
                "raw-admin-token";

        MockServerWebExchange exchange =
                createExchangeWithAuthorizationHeader(
                        token
                );

        when(jwtService.extractEmail(token))
                .thenReturn("admin@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(token))
                .thenReturn(1);

        AtomicReference<Authentication> capturedAuthentication =
                captureAuthentication(exchange);

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
                containsAuthority(
                        authentication,
                        "ROLE_ADMIN"
                )
        );
    }

    // =========================================================
    // ROLE PREFIX NORMALIZATION
    // =========================================================

    @Test
    void filter_ShouldNormalizeRole_WhenRoleAlreadyHasPrefix() {

        String token =
                "prefixed-admin-token";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

        when(jwtService.extractEmail(token))
                .thenReturn("admin@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("ROLE_ADMIN");

        when(jwtService.extractUserId(token))
                .thenReturn(1);

        AtomicReference<Authentication> capturedAuthentication =
                captureAuthentication(exchange);

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

        AuthenticatedUser authenticatedUser =
                (AuthenticatedUser)
                        authentication.getPrincipal();

        assertEquals(
                "ADMIN",
                authenticatedUser.role()
        );

        assertTrue(
                containsAuthority(
                        authentication,
                        "ROLE_ADMIN"
                )
        );

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .noneMatch(authority ->
                                "ROLE_ROLE_ADMIN".equals(
                                        authority.getAuthority()
                                )
                        )
        );
    }

    // =========================================================
    // LOWERCASE ROLE
    // =========================================================

    @Test
    void filter_ShouldNormalizeRoleToUppercase() {

        String token =
                "lowercase-buyer-token";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

        when(jwtService.extractEmail(token))
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("buyer");

        when(jwtService.extractUserId(token))
                .thenReturn(4);

        AtomicReference<Authentication> capturedAuthentication =
                captureAuthentication(exchange);

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

        AuthenticatedUser authenticatedUser =
                (AuthenticatedUser)
                        authentication.getPrincipal();

        assertEquals(
                "BUYER",
                authenticatedUser.role()
        );

        assertTrue(
                containsAuthority(
                        authentication,
                        "ROLE_BUYER"
                )
        );
    }

    // =========================================================
    // COOKIE PRIORITY
    // =========================================================

    @Test
    void filter_ShouldPreferCookieToken_OverAuthorizationHeader() {

        String cookieToken =
                "cookie-buyer-token";

        String headerToken =
                "header-admin-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get(
                                "/api/v1/claims/claim-101"
                        )
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
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(cookieToken))
                .thenReturn("BUYER");

        when(jwtService.extractUserId(cookieToken))
                .thenReturn(4);

        AtomicReference<Authentication> capturedAuthentication =
                captureAuthentication(exchange);

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

        AuthenticatedUser authenticatedUser =
                (AuthenticatedUser)
                        authentication.getPrincipal();

        assertEquals(
                "BUYER",
                authenticatedUser.role()
        );

        verify(jwtService)
                .extractEmail(cookieToken);

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

        String token =
                "invalid-token";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

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
    // MISSING EMAIL
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenEmailIsMissing() {

        String token =
                "token-without-email";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

        when(jwtService.extractEmail(token))
                .thenReturn(null);

        when(jwtService.extractRole(token))
                .thenReturn("BUYER");

        when(jwtService.extractUserId(token))
                .thenReturn(4);

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
    // BLANK EMAIL
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenEmailIsBlank() {

        String token =
                "token-with-blank-email";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

        when(jwtService.extractEmail(token))
                .thenReturn("   ");

        when(jwtService.extractRole(token))
                .thenReturn("BUYER");

        when(jwtService.extractUserId(token))
                .thenReturn(4);

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
    // MISSING ROLE
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenRoleIsMissing() {

        String token =
                "token-without-role";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

        when(jwtService.extractEmail(token))
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn(null);

        when(jwtService.extractUserId(token))
                .thenReturn(4);

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
    // BLANK ROLE
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenRoleIsBlank() {

        String token =
                "token-with-blank-role";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

        when(jwtService.extractEmail(token))
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("   ");

        when(jwtService.extractUserId(token))
                .thenReturn(4);

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
    // MISSING USER ID
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenUserIdIsMissing() {

        String token =
                "token-without-user-id";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

        when(jwtService.extractEmail(token))
                .thenReturn("buyer@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("BUYER");

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

        MockServerWebExchange exchange =
                createExchangeWithCookie("");

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

    // =========================================================
    // HELPERS
    // =========================================================

    private MockServerWebExchange createExchangeWithoutToken() {

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get(
                                "/api/v1/claims/claim-101"
                        )
                        .build();

        return MockServerWebExchange.from(request);
    }

    private MockServerWebExchange createExchangeWithCookie(
            String token
    ) {
        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get(
                                "/api/v1/claims/claim-101"
                        )
                        .cookie(
                                new HttpCookie(
                                        "accessToken",
                                        token
                                )
                        )
                        .build();

        return MockServerWebExchange.from(request);
    }

    private MockServerWebExchange
    createExchangeWithAuthorizationHeader(
            String authorizationHeader
    ) {
        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get(
                                "/api/v1/claims/claim-101"
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                authorizationHeader
                        )
                        .build();

        return MockServerWebExchange.from(request);
    }

    private AtomicReference<Authentication>
    captureAuthentication(
            MockServerWebExchange exchange
    ) {
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

        return capturedAuthentication;
    }

    private boolean containsAuthority(
            Authentication authentication,
            String expectedAuthority
    ) {
        return authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        expectedAuthority.equals(
                                authority.getAuthority()
                        )
                );
    }
}