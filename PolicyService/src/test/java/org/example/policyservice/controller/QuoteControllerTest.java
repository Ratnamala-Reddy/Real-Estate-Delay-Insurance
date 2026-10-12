package org.example.policyservice.controller;

import org.example.policyservice.dto.GenerateQuoteRequest;
import org.example.policyservice.entity.Quote;
import org.example.policyservice.enums.QuoteStatus;
import org.example.policyservice.exception.QuoteNotFoundException;
import org.example.policyservice.security.AuthenticatedUser;
import org.example.policyservice.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteControllerTest {

    @Mock
    private PolicyService policyService;

    private QuoteController quoteController;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final Integer BUYER_USER_ID =
            4;

    private AuthenticatedUser buyer;

    private AuthenticatedUser admin;

    @BeforeEach
    void setUp() {

        quoteController =
                new QuoteController(
                        policyService
                );

        buyer =
                new AuthenticatedUser(
                        BUYER_USER_ID,
                        "buyer@gmail.com",
                        "BUYER"
                );

        admin =
                new AuthenticatedUser(
                        1,
                        "admin@gmail.com",
                        "ADMIN"
                );
    }

    // =========================================================
    // GENERATE QUOTES
    // =========================================================

    @Test
    void generateQuotes_ShouldSetApplicationIdAndReturnThreeQuotes() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        request.setApplicationId(
                "different-application-id"
        );

        Quote basic =
                createQuote(
                        "quote-basic",
                        "BASIC",
                        QuoteStatus.GENERATED.name()
                );

        Quote standard =
                createQuote(
                        "quote-standard",
                        "STANDARD",
                        QuoteStatus.GENERATED.name()
                );

        Quote premium =
                createQuote(
                        "quote-premium",
                        "PREMIUM",
                        QuoteStatus.GENERATED.name()
                );

        when(policyService.generateQuotes(request))
                .thenReturn(
                        Flux.just(
                                basic,
                                standard,
                                premium
                        )
                );

        StepVerifier.create(
                        quoteController.generateQuotes(
                                APPLICATION_ID,
                                request
                        )
                )
                .assertNext(quote ->
                        assertEquals(
                                "BASIC",
                                quote.getPlanName()
                        )
                )
                .assertNext(quote ->
                        assertEquals(
                                "STANDARD",
                                quote.getPlanName()
                        )
                )
                .assertNext(quote ->
                        assertEquals(
                                "PREMIUM",
                                quote.getPlanName()
                        )
                )
                .verifyComplete();

        assertEquals(
                APPLICATION_ID,
                request.getApplicationId()
        );

        verify(policyService)
                .generateQuotes(request);
    }

    @Test
    void generateQuotes_ShouldPropagateServiceError() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        RuntimeException exception =
                new RuntimeException(
                        "Quote generation failed"
                );

        when(policyService.generateQuotes(request))
                .thenReturn(
                        Flux.error(exception)
                );

        StepVerifier.create(
                        quoteController.generateQuotes(
                                APPLICATION_ID,
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Quote generation failed"
                        )
                )
                .verify();

        assertEquals(
                APPLICATION_ID,
                request.getApplicationId()
        );

        verify(policyService)
                .generateQuotes(request);
    }

    @Test
    void generateQuotes_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        when(policyService.generateQuotes(request))
                .thenReturn(
                        Flux.empty()
                );

        StepVerifier.create(
                        quoteController.generateQuotes(
                                APPLICATION_ID,
                                request
                        )
                )
                .verifyComplete();

        assertEquals(
                APPLICATION_ID,
                request.getApplicationId()
        );

        verify(policyService)
                .generateQuotes(request);
    }

    // =========================================================
    // GET QUOTES
    // =========================================================

    @Test
    void getQuotes_ShouldReturnQuotesForBuyer() {

        Quote basic =
                createQuote(
                        "quote-basic",
                        "BASIC",
                        QuoteStatus.GENERATED.name()
                );

        Quote standard =
                createQuote(
                        "quote-standard",
                        "STANDARD",
                        QuoteStatus.GENERATED.name()
                );

        when(policyService.getQuotes(
                APPLICATION_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Flux.just(
                        basic,
                        standard
                )
        );

        StepVerifier.create(
                        quoteController.getQuotes(
                                APPLICATION_ID,
                                buyer
                        )
                )
                .assertNext(quote -> {
                    assertEquals(
                            "quote-basic",
                            quote.getQuoteId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            quote.getApplicationId()
                    );
                })
                .assertNext(quote ->
                        assertEquals(
                                "quote-standard",
                                quote.getQuoteId()
                        )
                )
                .verifyComplete();

        verify(policyService)
                .getQuotes(
                        APPLICATION_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void getQuotes_ShouldUseAdminIdentity() {

        Quote quote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.GENERATED.name()
                );

        when(policyService.getQuotes(
                APPLICATION_ID,
                1,
                "ADMIN"
        )).thenReturn(
                Flux.just(quote)
        );

        StepVerifier.create(
                        quoteController.getQuotes(
                                APPLICATION_ID,
                                admin
                        )
                )
                .expectNext(quote)
                .verifyComplete();

        verify(policyService)
                .getQuotes(
                        APPLICATION_ID,
                        1,
                        "ADMIN"
                );
    }

    @Test
    void getQuotes_ShouldPropagateOwnershipError() {

        RuntimeException exception =
                new RuntimeException(
                        "You do not own this application"
                );

        when(policyService.getQuotes(
                APPLICATION_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Flux.error(exception)
        );

        StepVerifier.create(
                        quoteController.getQuotes(
                                APPLICATION_ID,
                                buyer
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "You do not own this application"
                        )
                )
                .verify();

        verify(policyService)
                .getQuotes(
                        APPLICATION_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    // =========================================================
    // GET SINGLE QUOTE
    // =========================================================

    @Test
    void getQuote_ShouldReturnQuoteForBuyer() {

        Quote quote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.GENERATED.name()
                );

        when(policyService.getQuote(
                QUOTE_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.just(quote)
        );

        StepVerifier.create(
                        quoteController.getQuote(
                                QUOTE_ID,
                                buyer
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            QUOTE_ID,
                            response.getQuoteId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            response.getApplicationId()
                    );

                    assertEquals(
                            "STANDARD",
                            response.getPlanName()
                    );

                    assertEquals(
                            QuoteStatus.GENERATED.name(),
                            response.getStatus()
                    );
                })
                .verifyComplete();

        verify(policyService)
                .getQuote(
                        QUOTE_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void getQuote_ShouldPropagateNotFoundError() {

        QuoteNotFoundException exception =
                new QuoteNotFoundException(
                        "Quote not found with ID: "
                                + QUOTE_ID
                );

        when(policyService.getQuote(
                QUOTE_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        quoteController.getQuote(
                                QUOTE_ID,
                                buyer
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Quote not found with ID: "
                                        + QUOTE_ID
                        )
                )
                .verify();

        verify(policyService)
                .getQuote(
                        QUOTE_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void getQuote_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        when(policyService.getQuote(
                QUOTE_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        quoteController.getQuote(
                                QUOTE_ID,
                                buyer
                        )
                )
                .verifyComplete();

        verify(policyService)
                .getQuote(
                        QUOTE_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    // =========================================================
    // ACCEPT QUOTE
    // =========================================================

    @Test
    void acceptQuote_ShouldReturnAcceptedQuote() {

        Quote acceptedQuote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.ACCEPTED.name()
                );

        acceptedQuote.setNew(false);

        when(policyService.acceptQuote(
                QUOTE_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.just(acceptedQuote)
        );

        StepVerifier.create(
                        quoteController.acceptQuote(
                                QUOTE_ID,
                                buyer
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            QUOTE_ID,
                            response.getQuoteId()
                    );

                    assertEquals(
                            QuoteStatus.ACCEPTED.name(),
                            response.getStatus()
                    );

                    assertFalse(
                            response.isNew()
                    );
                })
                .verifyComplete();

        verify(policyService)
                .acceptQuote(
                        QUOTE_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void acceptQuote_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Quote is already finalized as ACCEPTED"
                );

        when(policyService.acceptQuote(
                QUOTE_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        quoteController.acceptQuote(
                                QUOTE_ID,
                                buyer
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Quote is already finalized as ACCEPTED"
                        )
                )
                .verify();

        verify(policyService)
                .acceptQuote(
                        QUOTE_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void acceptQuote_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        when(policyService.acceptQuote(
                QUOTE_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        quoteController.acceptQuote(
                                QUOTE_ID,
                                buyer
                        )
                )
                .verifyComplete();

        verify(policyService)
                .acceptQuote(
                        QUOTE_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    // =========================================================
    // DECLINE QUOTE
    // =========================================================

    @Test
    void declineQuote_ShouldReturnDeclinedQuote() {

        Quote declinedQuote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.DECLINED.name()
                );

        declinedQuote.setNew(false);

        when(policyService.declineQuote(
                QUOTE_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.just(declinedQuote)
        );

        StepVerifier.create(
                        quoteController.declineQuote(
                                QUOTE_ID,
                                buyer
                        )
                )
                .assertNext(response -> {
                    assertEquals(
                            QUOTE_ID,
                            response.getQuoteId()
                    );

                    assertEquals(
                            QuoteStatus.DECLINED.name(),
                            response.getStatus()
                    );

                    assertFalse(
                            response.isNew()
                    );
                })
                .verifyComplete();

        verify(policyService)
                .declineQuote(
                        QUOTE_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void declineQuote_ShouldPropagateServiceError() {

        RuntimeException exception =
                new RuntimeException(
                        "Quote is already finalized as DECLINED"
                );

        when(policyService.declineQuote(
                QUOTE_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.error(exception)
        );

        StepVerifier.create(
                        quoteController.declineQuote(
                                QUOTE_ID,
                                buyer
                        )
                )
                .expectErrorMatches(error ->
                        error == exception
                                && error.getMessage().equals(
                                "Quote is already finalized as DECLINED"
                        )
                )
                .verify();

        verify(policyService)
                .declineQuote(
                        QUOTE_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    @Test
    void declineQuote_ShouldCompleteEmpty_WhenServiceReturnsEmpty() {

        when(policyService.declineQuote(
                QUOTE_ID,
                BUYER_USER_ID,
                "BUYER"
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        quoteController.declineQuote(
                                QUOTE_ID,
                                buyer
                        )
                )
                .verifyComplete();

        verify(policyService)
                .declineQuote(
                        QUOTE_ID,
                        BUYER_USER_ID,
                        "BUYER"
                );
    }

    // =========================================================
    // TEST DATA HELPERS
    // =========================================================

    private GenerateQuoteRequest createGenerateQuoteRequest() {

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
                LocalDateTime.of(
                        2026,
                        11,
                        3,
                        22,
                        29
                )
        );

        quote.setStatus(
                status
        );

        quote.setCreatedAt(
                LocalDateTime.of(
                        2026,
                        10,
                        4,
                        22,
                        29
                )
        );

        quote.setNew(
                QuoteStatus.GENERATED.name()
                        .equals(status)
        );

        return quote;
    }
}