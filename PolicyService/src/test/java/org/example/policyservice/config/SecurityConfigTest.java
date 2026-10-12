package org.example.policyservice.config;

import org.example.policyservice.controller.QuoteController;
import org.example.policyservice.dto.GenerateQuoteRequest;
import org.example.policyservice.entity.Quote;
import org.example.policyservice.enums.QuoteStatus;
import org.example.policyservice.exception.GlobalExceptionHandler;
import org.example.policyservice.security.JwtService;
import org.example.policyservice.service.PolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(
        controllers = QuoteController.class
)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PolicyService policyService;

    @MockitoBean
    private JwtService jwtService;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String ADMIN_TOKEN =
            "valid-admin-token";

    private static final String BUYER_TOKEN =
            "valid-buyer-token";

    // =========================================================
    // MISSING TOKEN
    // =========================================================

    @Test
    void generateQuotes_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .post()
                .uri(
                        "/api/v1/quotes/applications/"
                                + APPLICATION_ID
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        createGenerateQuoteRequest()
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(policyService, never())
                .generateQuotes(
                        any(GenerateQuoteRequest.class)
                );
    }

    @Test
    void getQuotes_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .get()
                .uri(
                        "/api/v1/quotes/applications/"
                                + APPLICATION_ID
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(policyService, never())
                .getQuotes(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    @Test
    void getQuote_ShouldReturnUnauthorized_WhenTokenIsMissing() {

        webTestClient
                .get()
                .uri(
                        "/api/v1/quotes/"
                                + QUOTE_ID
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(policyService, never())
                .getQuote(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // GENERATE QUOTES
    // =========================================================

    @Test
    void generateQuotes_ShouldReturnOk_WhenRoleIsAdmin() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        mockAdminClaims();

        when(policyService.generateQuotes(
                any(GenerateQuoteRequest.class)
        )).thenReturn(
                Flux.just(
                        createQuote(
                                "quote-basic",
                                "BASIC",
                                QuoteStatus.GENERATED.name()
                        ),
                        createQuote(
                                "quote-standard",
                                "STANDARD",
                                QuoteStatus.GENERATED.name()
                        ),
                        createQuote(
                                "quote-premium",
                                "PREMIUM",
                                QuoteStatus.GENERATED.name()
                        )
                )
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/quotes/applications/"
                                + APPLICATION_ID
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].planName")
                .isEqualTo("BASIC")
                .jsonPath("$[1].planName")
                .isEqualTo("STANDARD")
                .jsonPath("$[2].planName")
                .isEqualTo("PREMIUM");

        verify(policyService)
                .generateQuotes(
                        any(GenerateQuoteRequest.class)
                );
    }

    @Test
    void generateQuotes_ShouldReturnForbidden_WhenRoleIsBuyer() {

        mockBuyerClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/quotes/applications/"
                                + APPLICATION_ID
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        createGenerateQuoteRequest()
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(policyService, never())
                .generateQuotes(
                        any(GenerateQuoteRequest.class)
                );
    }

    // =========================================================
    // GET QUOTES
    // =========================================================

    @Test
    void getQuotes_ShouldReturnOk_WhenRoleIsAdmin() {

        mockAdminClaims();

        when(policyService.getQuotes(
                APPLICATION_ID,
                1,
                "ADMIN"
        )).thenReturn(
                Flux.just(
                        createQuote(
                                QUOTE_ID,
                                "STANDARD",
                                QuoteStatus.GENERATED.name()
                        )
                )
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/quotes/applications/"
                                + APPLICATION_ID
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].quoteId")
                .isEqualTo(QUOTE_ID)
                .jsonPath("$[0].applicationId")
                .isEqualTo(APPLICATION_ID)
                .jsonPath("$[0].status")
                .isEqualTo("GENERATED");

        verify(policyService)
                .getQuotes(
                        APPLICATION_ID,
                        1,
                        "ADMIN"
                );
    }

    @Test
    void getQuotes_ShouldReturnOk_WhenRoleIsBuyer() {

        mockBuyerClaims();

        when(policyService.getQuotes(
                APPLICATION_ID,
                4,
                "BUYER"
        )).thenReturn(
                Flux.just(
                        createQuote(
                                QUOTE_ID,
                                "STANDARD",
                                QuoteStatus.GENERATED.name()
                        )
                )
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/quotes/applications/"
                                + APPLICATION_ID
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].quoteId")
                .isEqualTo(QUOTE_ID)
                .jsonPath("$[0].status")
                .isEqualTo("GENERATED");

        verify(policyService)
                .getQuotes(
                        APPLICATION_ID,
                        4,
                        "BUYER"
                );
    }

    // =========================================================
    // GET SINGLE QUOTE
    // =========================================================

    @Test
    void getQuote_ShouldReturnOk_WhenRoleIsAdmin() {

        mockAdminClaims();

        when(policyService.getQuote(
                QUOTE_ID,
                1,
                "ADMIN"
        )).thenReturn(
                Mono.just(
                        createQuote(
                                QUOTE_ID,
                                "STANDARD",
                                QuoteStatus.GENERATED.name()
                        )
                )
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/quotes/"
                                + QUOTE_ID
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.quoteId")
                .isEqualTo(QUOTE_ID)
                .jsonPath("$.planName")
                .isEqualTo("STANDARD")
                .jsonPath("$.status")
                .isEqualTo("GENERATED");

        verify(policyService)
                .getQuote(
                        QUOTE_ID,
                        1,
                        "ADMIN"
                );
    }

    @Test
    void getQuote_ShouldReturnOk_WhenRoleIsBuyer() {

        mockBuyerClaims();

        when(policyService.getQuote(
                QUOTE_ID,
                4,
                "BUYER"
        )).thenReturn(
                Mono.just(
                        createQuote(
                                QUOTE_ID,
                                "STANDARD",
                                QuoteStatus.GENERATED.name()
                        )
                )
        );

        webTestClient
                .get()
                .uri(
                        "/api/v1/quotes/"
                                + QUOTE_ID
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.quoteId")
                .isEqualTo(QUOTE_ID)
                .jsonPath("$.applicationId")
                .isEqualTo(APPLICATION_ID);

        verify(policyService)
                .getQuote(
                        QUOTE_ID,
                        4,
                        "BUYER"
                );
    }

    // =========================================================
    // ACCEPT QUOTE
    // =========================================================

    @Test
    void acceptQuote_ShouldReturnOk_WhenRoleIsBuyer() {

        Quote acceptedQuote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.ACCEPTED.name()
                );

        acceptedQuote.setNew(false);

        mockBuyerClaims();

        when(policyService.acceptQuote(
                QUOTE_ID,
                4,
                "BUYER"
        )).thenReturn(
                Mono.just(acceptedQuote)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/quotes/"
                                + QUOTE_ID
                                + "/accept"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.quoteId")
                .isEqualTo(QUOTE_ID)
                .jsonPath("$.status")
                .isEqualTo("ACCEPTED");

        verify(policyService)
                .acceptQuote(
                        QUOTE_ID,
                        4,
                        "BUYER"
                );
    }

    @Test
    void acceptQuote_ShouldReturnForbidden_WhenRoleIsAdmin() {

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/quotes/"
                                + QUOTE_ID
                                + "/accept"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(policyService, never())
                .acceptQuote(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // DECLINE QUOTE
    // =========================================================

    @Test
    void declineQuote_ShouldReturnOk_WhenRoleIsBuyer() {

        Quote declinedQuote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.DECLINED.name()
                );

        declinedQuote.setNew(false);

        mockBuyerClaims();

        when(policyService.declineQuote(
                QUOTE_ID,
                4,
                "BUYER"
        )).thenReturn(
                Mono.just(declinedQuote)
        );

        webTestClient
                .post()
                .uri(
                        "/api/v1/quotes/"
                                + QUOTE_ID
                                + "/decline"
                )
                .cookie(
                        "accessToken",
                        BUYER_TOKEN
                )
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.quoteId")
                .isEqualTo(QUOTE_ID)
                .jsonPath("$.status")
                .isEqualTo("DECLINED");

        verify(policyService)
                .declineQuote(
                        QUOTE_ID,
                        4,
                        "BUYER"
                );
    }

    @Test
    void declineQuote_ShouldReturnForbidden_WhenRoleIsAdmin() {

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/quotes/"
                                + QUOTE_ID
                                + "/decline"
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(policyService, never())
                .declineQuote(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // INVALID TOKEN
    // =========================================================

    @Test
    void quoteEndpoint_ShouldReturnUnauthorized_WhenTokenIsInvalid() {

        String invalidToken =
                "invalid-token";

        when(jwtService.extractEmail(invalidToken))
                .thenThrow(
                        new IllegalArgumentException(
                                "Invalid JWT"
                        )
                );

        webTestClient
                .get()
                .uri(
                        "/api/v1/quotes/"
                                + QUOTE_ID
                )
                .cookie(
                        "accessToken",
                        invalidToken
                )
                .exchange()
                .expectStatus()
                .isUnauthorized();

        verify(policyService, never())
                .getQuote(
                        anyString(),
                        anyInt(),
                        anyString()
                );
    }

    // =========================================================
    // UNSUPPORTED ROLE
    // =========================================================

    @Test
    void generateQuotes_ShouldReturnForbidden_WhenRoleIsUnsupported() {

        String token =
                "unsupported-role-token";

        when(jwtService.extractEmail(token))
                .thenReturn("claims@gmail.com");

        when(jwtService.extractRole(token))
                .thenReturn("CLAIMS_OFFICER");

        when(jwtService.extractUserId(token))
                .thenReturn(10);

        webTestClient
                .post()
                .uri(
                        "/api/v1/quotes/applications/"
                                + APPLICATION_ID
                )
                .cookie(
                        "accessToken",
                        token
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(
                        createGenerateQuoteRequest()
                )
                .exchange()
                .expectStatus()
                .isForbidden();

        verify(policyService, never())
                .generateQuotes(
                        any(GenerateQuoteRequest.class)
                );
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    @Test
    void generateQuotes_ShouldReturnBadRequest_WhenCoverageIsZero() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        request.setCoverageAmount(
                0.0
        );

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/quotes/applications/"
                                + APPLICATION_ID
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(policyService, never())
                .generateQuotes(
                        any(GenerateQuoteRequest.class)
                );
    }

    @Test
    void generateQuotes_ShouldReturnBadRequest_WhenGeneratedByIsBlank() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        request.setGeneratedBy("");

        mockAdminClaims();

        webTestClient
                .post()
                .uri(
                        "/api/v1/quotes/applications/"
                                + APPLICATION_ID
                )
                .cookie(
                        "accessToken",
                        ADMIN_TOKEN
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .bodyValue(request)
                .exchange()
                .expectStatus()
                .isBadRequest();

        verify(policyService, never())
                .generateQuotes(
                        any(GenerateQuoteRequest.class)
                );
    }

    // =========================================================
    // AUTHENTICATION HELPERS
    // =========================================================

    private void mockAdminClaims() {

        when(jwtService.extractEmail(
                ADMIN_TOKEN
        )).thenReturn(
                "admin@gmail.com"
        );

        when(jwtService.extractRole(
                ADMIN_TOKEN
        )).thenReturn(
                "ADMIN"
        );

        when(jwtService.extractUserId(
                ADMIN_TOKEN
        )).thenReturn(
                1
        );
    }

    private void mockBuyerClaims() {

        when(jwtService.extractEmail(
                BUYER_TOKEN
        )).thenReturn(
                "buyer@gmail.com"
        );

        when(jwtService.extractRole(
                BUYER_TOKEN
        )).thenReturn(
                "BUYER"
        );

        when(jwtService.extractUserId(
                BUYER_TOKEN
        )).thenReturn(
                4
        );
    }

    // =========================================================
    // REQUEST HELPER
    // =========================================================

    private GenerateQuoteRequest
    createGenerateQuoteRequest() {

        GenerateQuoteRequest request =
                new GenerateQuoteRequest();

        request.setApplicationId(
                APPLICATION_ID
        );

        request.setCoverageAmount(
                1200000.0
        );

        request.setGeneratedBy(
                "ADMIN"
        );

        return request;
    }

    // =========================================================
    // QUOTE HELPER
    // =========================================================

    private Quote createQuote(
            String quoteId,
            String planName,
            String status
    ) {
        Quote quote =
                new Quote();

        quote.setQuoteId(
                quoteId
        );

        quote.setApplicationId(
                APPLICATION_ID
        );

        quote.setPlanName(
                planName
        );

        quote.setPremiumAmount(
                37500.0
        );

        quote.setMonthlyCompensation(
                50000.0
        );

        quote.setMaximumDelayMonths(
                24
        );

        quote.setMaximumPayout(
                1200000.0
        );

        quote.setGracePeriodDays(
                90
        );

        quote.setExpiryDate(
                LocalDateTime.now()
                        .plusDays(30)
        );

        quote.setStatus(
                status
        );

        quote.setCreatedAt(
                LocalDateTime.now()
        );

        quote.setNew(
                QuoteStatus.GENERATED.name()
                        .equals(status)
        );

        return quote;
    }
}