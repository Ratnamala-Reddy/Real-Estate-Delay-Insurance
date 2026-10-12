package org.example.policyservice.service;

import org.example.policyservice.dto.GenerateQuoteRequest;
import org.example.policyservice.entity.Quote;
import org.example.policyservice.enums.QuoteStatus;
import org.example.policyservice.exception.InvalidQuoteRequestException;
import org.example.policyservice.exception.QuoteAlreadyFinalizedException;
import org.example.policyservice.exception.QuoteExpiredException;
import org.example.policyservice.exception.QuoteNotFoundException;
import org.example.policyservice.exception.QuotesAlreadyGeneratedException;
import org.example.policyservice.repository.PolicyRepository;
import org.example.policyservice.repository.QuoteRepository;
import org.example.policyservice.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceImplQuoteTest {

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private WebClient webClient;

    @Mock
    private JwtService jwtService;

    private PolicyServiceImpl policyService;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final Integer BUYER_USER_ID =
            4;

    @BeforeEach
    void setUp() {

        StepVerifier.setDefaultTimeout(
                Duration.ofSeconds(5)
        );

        policyService =
                new PolicyServiceImpl(
                        quoteRepository,
                        policyRepository,
                        template,
                        webClient,
                        jwtService
                );

        ReflectionTestUtils.setField(
                policyService,
                "applicationServiceUrl",
                "http://localhost:8082"
        );

        ReflectionTestUtils.setField(
                policyService,
                "riskServiceUrl",
                "http://localhost:8084"
        );
    }

    @AfterEach
    void tearDown() {
        StepVerifier.resetDefaultTimeout();
    }

    // =========================================================
    // GENERATE QUOTES VALIDATION
    // =========================================================

    @Test
    void generateQuotes_ShouldThrowException_WhenRequestIsNull() {

        StepVerifier.create(
                        Flux.defer(() ->
                                policyService.generateQuotes(null)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidQuoteRequestException
                                && error.getMessage().equals(
                                "Generate quote request is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();

        verify(quoteRepository, never())
                .existsByApplicationId(any(String.class));
    }

    @Test
    void generateQuotes_ShouldThrowException_WhenApplicationIdIsNull() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        request.setApplicationId(null);

        StepVerifier.create(
                        Flux.defer(() ->
                                policyService.generateQuotes(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidQuoteRequestException
                                && error.getMessage().equals(
                                "Application ID is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void generateQuotes_ShouldThrowException_WhenApplicationIdIsBlank() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        request.setApplicationId("   ");

        StepVerifier.create(
                        Flux.defer(() ->
                                policyService.generateQuotes(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidQuoteRequestException
                                && error.getMessage().equals(
                                "Application ID is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void generateQuotes_ShouldThrowException_WhenGeneratedByIsBlank() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        request.setGeneratedBy("");

        StepVerifier.create(
                        Flux.defer(() ->
                                policyService.generateQuotes(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidQuoteRequestException
                                && error.getMessage().equals(
                                "Generated by is required"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void generateQuotes_ShouldThrowException_WhenCoverageIsZero() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        request.setCoverageAmount(0.0);

        StepVerifier.create(
                        Flux.defer(() ->
                                policyService.generateQuotes(request)
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidQuoteRequestException
                                && error.getMessage().equals(
                                "Coverage amount must be greater than zero"
                        )
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void generateQuotes_ShouldThrowException_WhenCoverageIsNegative() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        request.setCoverageAmount(-1.0);

        StepVerifier.create(
                        Flux.defer(() ->
                                policyService.generateQuotes(request)
                        )
                )
                .expectError(
                        InvalidQuoteRequestException.class
                )
                .verify();

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void generateQuotes_ShouldThrowException_WhenQuotesAlreadyExist() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        when(jwtService.generateServiceToken())
                .thenReturn("service-token");

        when(quoteRepository.existsByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(true)
        );

        StepVerifier.create(
                        policyService.generateQuotes(request)
                )
                .expectErrorMatches(error ->
                        error instanceof QuotesAlreadyGeneratedException
                                && error.getMessage().equals(
                                "Quotes are already generated "
                                        + "for application: "
                                        + APPLICATION_ID
                        )
                )
                .verify();

        verify(quoteRepository)
                .existsByApplicationId(APPLICATION_ID);

        verify(template, never())
                .insert(Quote.class);
    }

    // =========================================================
    // GET QUOTES AS ADMIN
    // =========================================================

    @Test
    void getQuotes_ShouldReturnQuotes_WhenCallerIsAdmin() {

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

        when(quoteRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Flux.just(
                        basic,
                        standard
                )
        );

        StepVerifier.create(
                        policyService.getQuotes(
                                APPLICATION_ID,
                                1,
                                "ADMIN"
                        )
                )
                .expectNext(
                        basic,
                        standard
                )
                .verifyComplete();

        verify(quoteRepository)
                .findByApplicationId(APPLICATION_ID);

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void getQuotes_ShouldThrowException_WhenApplicationIdIsBlank() {

        StepVerifier.create(
                        Flux.defer(() ->
                                policyService.getQuotes(
                                        " ",
                                        1,
                                        "ADMIN"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidQuoteRequestException
                                && error.getMessage().equals(
                                "Application ID is required"
                        )
                )
                .verify();

        verify(quoteRepository, never())
                .findByApplicationId(any(String.class));
    }

    // =========================================================
    // GET SINGLE QUOTE
    // =========================================================

    @Test
    void getQuote_ShouldReturnQuote_WhenCallerIsAdmin() {

        Quote quote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.GENERATED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        StepVerifier.create(
                        policyService.getQuote(
                                QUOTE_ID,
                                1,
                                "ADMIN"
                        )
                )
                .assertNext(result -> {
                    assertEquals(
                            QUOTE_ID,
                            result.getQuoteId()
                    );

                    assertEquals(
                            APPLICATION_ID,
                            result.getApplicationId()
                    );

                    assertEquals(
                            QuoteStatus.GENERATED.name(),
                            result.getStatus()
                    );
                })
                .verifyComplete();

        verify(quoteRepository)
                .findById(QUOTE_ID);

        verify(jwtService, never())
                .generateServiceToken();
    }

    @Test
    void getQuote_ShouldUseFallbackRepositoryMethod_WhenFindByIdIsEmpty() {

        Quote quote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.GENERATED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.empty()
        );

        when(quoteRepository.findByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        StepVerifier.create(
                        policyService.getQuote(
                                QUOTE_ID,
                                1,
                                "ADMIN"
                        )
                )
                .expectNext(quote)
                .verifyComplete();

        verify(quoteRepository)
                .findByQuoteId(QUOTE_ID);
    }

    @Test
    void getQuote_ShouldThrowNotFound_WhenQuoteDoesNotExist() {

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.empty()
        );

        when(quoteRepository.findByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        policyService.getQuote(
                                QUOTE_ID,
                                1,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteNotFoundException
                                && error.getMessage().equals(
                                "Quote not found with ID: "
                                        + QUOTE_ID
                        )
                )
                .verify();
    }

    @Test
    void getQuote_ShouldThrowException_WhenQuoteIdIsBlank() {

        StepVerifier.create(
                        Mono.defer(() ->
                                policyService.getQuote(
                                        " ",
                                        1,
                                        "ADMIN"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidQuoteRequestException
                                && error.getMessage().equals(
                                "Quote ID is required"
                        )
                )
                .verify();

        verify(quoteRepository, never())
                .findById(any(String.class));
    }

    // =========================================================
    // ACCEPT QUOTE
    // =========================================================

    @Test
    void acceptQuote_ShouldAcceptGeneratedQuote_AsAdminServiceMethod() {

        Quote chosenQuote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.GENERATED.name()
                );

        Quote basicQuote =
                createQuote(
                        "quote-basic",
                        "BASIC",
                        QuoteStatus.GENERATED.name()
                );

        Quote premiumQuote =
                createQuote(
                        "quote-premium",
                        "PREMIUM",
                        QuoteStatus.GENERATED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(chosenQuote)
        );

        when(quoteRepository.findByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Flux.just(
                        basicQuote,
                        chosenQuote,
                        premiumQuote
                )
        );

        when(template.update(
                any(Quote.class)
        )).thenAnswer(invocation -> {
            Quote quote =
                    invocation.getArgument(0);

            return Mono.just(quote);
        });

        StepVerifier.create(
                        policyService.acceptQuote(
                                QUOTE_ID,
                                null,
                                "ADMIN"
                        )
                )
                .assertNext(result -> {
                    assertEquals(
                            QUOTE_ID,
                            result.getQuoteId()
                    );

                    assertEquals(
                            QuoteStatus.ACCEPTED.name(),
                            result.getStatus()
                    );

                    assertFalse(
                            result.isNew()
                    );
                })
                .verifyComplete();

        assertEquals(
                QuoteStatus.DECLINED.name(),
                basicQuote.getStatus()
        );

        assertEquals(
                QuoteStatus.DECLINED.name(),
                premiumQuote.getStatus()
        );

        verify(template)
                .update(chosenQuote);

        verify(template)
                .update(basicQuote);

        verify(template)
                .update(premiumQuote);
    }

    @Test
    void acceptQuote_ShouldThrowException_WhenQuoteIsExpired() {

        Quote quote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.GENERATED.name()
                );

        quote.setExpiryDate(
                LocalDateTime.now()
                        .minusDays(1)
        );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        StepVerifier.create(
                        policyService.acceptQuote(
                                QUOTE_ID,
                                null,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteExpiredException
                                && error.getMessage().equals(
                                "Quote has expired: "
                                        + QUOTE_ID
                        )
                )
                .verify();

        verify(template, never())
                .update(any(Quote.class));
    }

    @Test
    void acceptQuote_ShouldThrowException_WhenAlreadyAccepted() {

        Quote quote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.ACCEPTED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        StepVerifier.create(
                        policyService.acceptQuote(
                                QUOTE_ID,
                                null,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteAlreadyFinalizedException
                                && error.getMessage().equals(
                                "Quote is already finalized as ACCEPTED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(Quote.class));
    }

    @Test
    void acceptQuote_ShouldThrowException_WhenAlreadyDeclined() {

        Quote quote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.DECLINED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        StepVerifier.create(
                        policyService.acceptQuote(
                                QUOTE_ID,
                                null,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteAlreadyFinalizedException
                                && error.getMessage().equals(
                                "Quote is already finalized as DECLINED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(Quote.class));
    }

    @Test
    void acceptQuote_ShouldThrowException_WhenQuoteIdIsBlank() {

        StepVerifier.create(
                        Mono.defer(() ->
                                policyService.acceptQuote(
                                        "",
                                        BUYER_USER_ID,
                                        "BUYER"
                                )
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof InvalidQuoteRequestException
                                && error.getMessage().equals(
                                "Quote ID is required"
                        )
                )
                .verify();

        verify(quoteRepository, never())
                .findById(any(String.class));
    }

    // =========================================================
    // DECLINE QUOTE
    // =========================================================

    @Test
    void declineQuote_ShouldUpdateGeneratedQuote_AsAdminServiceMethod() {

        Quote quote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.GENERATED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        when(template.update(quote))
                .thenReturn(
                        Mono.just(quote)
                );

        StepVerifier.create(
                        policyService.declineQuote(
                                QUOTE_ID,
                                null,
                                "ADMIN"
                        )
                )
                .assertNext(result -> {
                    assertEquals(
                            QuoteStatus.DECLINED.name(),
                            result.getStatus()
                    );

                    assertFalse(
                            result.isNew()
                    );
                })
                .verifyComplete();

        verify(template)
                .update(quote);
    }

    @Test
    void declineQuote_ShouldThrowException_WhenAlreadyAccepted() {

        Quote quote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.ACCEPTED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        StepVerifier.create(
                        policyService.declineQuote(
                                QUOTE_ID,
                                null,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteAlreadyFinalizedException
                                && error.getMessage().equals(
                                "Quote is already finalized as ACCEPTED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(Quote.class));
    }

    @Test
    void declineQuote_ShouldThrowException_WhenAlreadyDeclined() {

        Quote quote =
                createQuote(
                        QUOTE_ID,
                        "STANDARD",
                        QuoteStatus.DECLINED.name()
                );

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        StepVerifier.create(
                        policyService.declineQuote(
                                QUOTE_ID,
                                null,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteAlreadyFinalizedException
                                && error.getMessage().equals(
                                "Quote is already finalized as DECLINED"
                        )
                )
                .verify();

        verify(template, never())
                .update(any(Quote.class));
    }

    @Test
    void declineQuote_ShouldThrowNotFound_WhenQuoteDoesNotExist() {

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.empty()
        );

        when(quoteRepository.findByQuoteId(
                QUOTE_ID
        )).thenReturn(
                Mono.empty()
        );

        StepVerifier.create(
                        policyService.declineQuote(
                                QUOTE_ID,
                                null,
                                "ADMIN"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof QuoteNotFoundException
                                && error.getMessage().equals(
                                "Quote not found with ID: "
                                        + QUOTE_ID
                        )
                )
                .verify();

        verify(template, never())
                .update(any(Quote.class));
    }

    // =========================================================
    // HELPERS
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