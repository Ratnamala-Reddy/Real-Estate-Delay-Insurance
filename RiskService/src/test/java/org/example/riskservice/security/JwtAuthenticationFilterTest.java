package org.example.riskservice.security;

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
    // ADMIN COOKIE TOKEN
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
                containsAuthority(
                        authentication,
                        "ROLE_ADMIN"
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

        assertEquals(
                "buyer@gmail.com",
                authentication.getPrincipal()
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
    // BEARER AUTHORIZATION HEADER
    // =========================================================

    @Test
    void filter_ShouldCreateAuthentication_FromBearerHeader() {

        String token =
                "valid-header-token";

        MockServerWebExchange exchange =
                createExchangeWithAuthorizationHeader(
                        "Bearer " + token
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

        assertEquals(
                "admin@gmail.com",
                authentication.getPrincipal()
        );

        assertTrue(
                containsAuthority(
                        authentication,
                        "ROLE_ADMIN"
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
    // EXISTING ROLE PREFIX
    // =========================================================

    @Test
    void filter_ShouldNotDuplicateRolePrefix_WhenRoleHasPrefix() {

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
    // COOKIE PRIORITY
    // =========================================================

    @Test
    void filter_ShouldPreferCookieToken_OverAuthorizationHeader() {

        String cookieToken =
                "cookie-admin-token";

        String headerToken =
                "header-buyer-token";

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get(
                                "/api/v1/risk-assessments"
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
                .thenReturn("admin@gmail.com");

        when(jwtService.extractRole(cookieToken))
                .thenReturn("ADMIN");

        when(jwtService.extractUserId(cookieToken))
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

        assertEquals(
                "admin@gmail.com",
                authentication.getPrincipal()
        );

        assertTrue(
                containsAuthority(
                        authentication,
                        "ROLE_ADMIN"
                )
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
    // MISSING ROLE
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenRoleIsMissing() {

        String token =
                "token-without-role";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

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
    // BLANK ROLE
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenRoleIsBlank() {

        String token =
                "token-with-blank-role";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

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
    // MISSING USER ID
    // =========================================================

    @Test
    void filter_ShouldContinueWithoutAuthentication_WhenUserIdIsMissing() {

        String token =
                "token-without-user-id";

        MockServerWebExchange exchange =
                createExchangeWithCookie(token);

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
    // TEST HELPERS
    // =========================================================

    private MockServerWebExchange createExchangeWithoutToken() {

        MockServerHttpRequest request =
                MockServerHttpRequest
                        .get(
                                "/api/v1/risk-assessments"
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
                                "/api/v1/risk-assessments"
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
                                "/api/v1/risk-assessments"
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
                                                capturedAuthentication
                                                        .set(
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