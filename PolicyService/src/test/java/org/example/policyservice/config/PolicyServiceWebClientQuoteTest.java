package org.example.policyservice.config;

import org.example.policyservice.dto.GenerateQuoteRequest;
import org.example.policyservice.entity.Quote;
import org.example.policyservice.enums.QuoteStatus;
import org.example.policyservice.exception.ApplicationNotEligibleForQuoteException;
import org.example.policyservice.exception.ApplicationServiceException;
import org.example.policyservice.exception.RiskAssessmentNotEligibleException;
import org.example.policyservice.exception.RiskServiceException;
import org.example.policyservice.repository.PolicyRepository;
import org.example.policyservice.repository.QuoteRepository;
import org.example.policyservice.security.JwtService;
import org.example.policyservice.service.PolicyServiceImpl;
import org.junit.jupiter.api.AfterEach;
import static org.mockito.Mockito.times;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.ReactiveInsertOperation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceWebClientQuoteTest {

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private R2dbcEntityTemplate template;

    @Mock
    private ReactiveInsertOperation.ReactiveInsert<Quote>
            quoteInsert;

    @Mock
    private JwtService jwtService;

    private PolicyServiceImpl policyService;

    private static final String APPLICATION_ID =
            "application-101";

    private static final String QUOTE_ID =
            "quote-101";

    private static final String SERVICE_TOKEN =
            "signed-policy-service-token";

    private static final Integer OWNER_USER_ID =
            4;

    @BeforeEach
    void setUp() {

        StepVerifier.setDefaultTimeout(
                Duration.ofSeconds(5)
        );

        WebClient webClient =
                WebClient.builder()
                        .exchangeFunction(
                                exchangeFunction
                        )
                        .build();

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
    // SUCCESSFUL QUOTE GENERATION
    // =========================================================

    @Test
    void generateQuotes_ShouldCreateThreePlans_WhenApplicationAndRiskAreEligible() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(quoteRepository.existsByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(false)
        );

        List<ClientRequest> capturedRequests =
                new ArrayList<>();

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            ClientRequest clientRequest =
                    invocation.getArgument(0);

            capturedRequests.add(
                    clientRequest
            );

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                verifiedApplicationJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            approvedRiskJson()
                    )
            );
        });

        when(template.insert(Quote.class))
                .thenReturn(
                        quoteInsert
                );

        when(quoteInsert.using(
                any(Quote.class)
        )).thenAnswer(invocation -> {

            Quote quote =
                    invocation.getArgument(0);

            return Mono.just(
                    quote
            );
        });

        StepVerifier.create(
                        policyService.generateQuotes(
                                request
                        )
                )
                .recordWith(ArrayList::new)
                .expectNextCount(3)
                .consumeRecordedWith(quotes -> {

                    assertEquals(
                            3,
                            quotes.size()
                    );

                    Quote basic =
                            findPlan(
                                    quotes,
                                    "BASIC"
                            );

                    Quote standard =
                            findPlan(
                                    quotes,
                                    "STANDARD"
                            );

                    Quote premium =
                            findPlan(
                                    quotes,
                                    "PREMIUM"
                            );

                    assertNotNull(basic);
                    assertNotNull(standard);
                    assertNotNull(premium);

                    assertEquals(
                            840000.0,
                            basic.getMaximumPayout()
                    );

                    assertEquals(
                            1200000.0,
                            standard.getMaximumPayout()
                    );

                    assertEquals(
                            1680000.0,
                            premium.getMaximumPayout()
                    );

                    assertEquals(
                            18900.0,
                            basic.getPremiumAmount()
                    );

                    assertEquals(
                            37500.0,
                            standard.getPremiumAmount()
                    );

                    assertEquals(
                            67200.0,
                            premium.getPremiumAmount()
                    );

                    assertEquals(
                            QuoteStatus.GENERATED.name(),
                            basic.getStatus()
                    );

                    assertEquals(
                            QuoteStatus.GENERATED.name(),
                            standard.getStatus()
                    );

                    assertEquals(
                            QuoteStatus.GENERATED.name(),
                            premium.getStatus()
                    );

                    assertTrue(
                            basic.isNew()
                    );

                    assertTrue(
                            standard.isNew()
                    );

                    assertTrue(
                            premium.isNew()
                    );
                })
                .verifyComplete();

        assertEquals(
                2,
                capturedRequests.size()
        );

        ClientRequest applicationRequest =
                capturedRequests.get(0);

        ClientRequest riskRequest =
                capturedRequests.get(1);

        assertEquals(
                HttpMethod.GET,
                applicationRequest.method()
        );

        assertEquals(
                "http://localhost:8082/api/v1/applications/"
                        + APPLICATION_ID,
                applicationRequest
                        .url()
                        .toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                applicationRequest
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        assertEquals(
                HttpMethod.GET,
                riskRequest.method()
        );

        assertEquals(
                "http://localhost:8084/api/v1/"
                        + "risk-assessments/applications/"
                        + APPLICATION_ID
                        + "/risk-assessment",
                riskRequest
                        .url()
                        .toString()
        );

        assertEquals(
                "Bearer " + SERVICE_TOKEN,
                riskRequest
                        .headers()
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        )
        );

        verify(jwtService)
                .generateServiceToken();

        verify(quoteInsert, times(3))
                .using(any(Quote.class));
    }

    // =========================================================
    // APPLICATION NOT VERIFIED
    // =========================================================

    @Test
    void generateQuotes_ShouldRejectApplication_WhenApplicationIsSubmitted() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(quoteRepository.existsByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(false)
        );

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                submittedApplicationJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            approvedRiskJson()
                    )
            );
        });

        StepVerifier.create(
                        policyService.generateQuotes(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                ApplicationNotEligibleForQuoteException
                                && error.getMessage().equals(
                                "Application must be VERIFIED "
                                        + "before quote generation. "
                                        + "Current status: SUBMITTED"
                        )
                )
                .verify();

        verify(template, never())
                .insert(Quote.class);
    }

    // =========================================================
    // APPLICATION ID MISMATCH
    // =========================================================

    @Test
    void generateQuotes_ShouldThrowApplicationServiceException_WhenApplicationIdDoesNotMatch() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(quoteRepository.existsByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(false)
        );

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                """
                                {
                                  "applicationId": "different-application",
                                  "userId": 4,
                                  "propertyValue": 5000000.00,
                                  "status": "VERIFIED"
                                }
                                """
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            approvedRiskJson()
                    )
            );
        });

        StepVerifier.create(
                        policyService.generateQuotes(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                ApplicationServiceException
                                && error.getMessage().equals(
                                "Application ID returned by "
                                        + "Application Service does not match"
                        )
                )
                .verify();

        verify(template, never())
                .insert(Quote.class);
    }

    // =========================================================
    // APPLICATION SERVICE NOT FOUND
    // =========================================================

    @Test
    void generateQuotes_ShouldMapApplicationNotFoundResponse() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(quoteRepository.existsByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(false)
        );

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.NOT_FOUND,
                                """
                                {
                                  "code": "APPLICATION_NOT_FOUND",
                                  "message": "Application not found"
                                }
                                """
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            approvedRiskJson()
                    )
            );
        });

        StepVerifier.create(
                        policyService.generateQuotes(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                ApplicationServiceException
                                && error.getMessage().equals(
                                "Application not found: "
                                        + APPLICATION_ID
                        )
                )
                .verify();

        verify(template, never())
                .insert(Quote.class);
    }

    // =========================================================
    // APPLICATION SERVICE CONNECTION FAILURE
    // =========================================================

    @Test
    void generateQuotes_ShouldMapApplicationServiceConnectionFailure() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(quoteRepository.existsByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(false)
        );

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.error(
                        new RuntimeException(
                                "Connection refused"
                        )
                )
        );

        StepVerifier.create(
                        policyService.generateQuotes(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                ApplicationServiceException
                                || error instanceof
                                RiskServiceException
                )
                .verify();

        verify(template, never())
                .insert(Quote.class);
    }

    // =========================================================
    // RISK NOT APPROVED
    // =========================================================

    @Test
    void generateQuotes_ShouldRejectRiskAssessment_WhenStatusIsPending() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(quoteRepository.existsByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(false)
        );

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                verifiedApplicationJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            pendingRiskJson()
                    )
            );
        });

        StepVerifier.create(
                        policyService.generateQuotes(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof
                                RiskAssessmentNotEligibleException
                                && error.getMessage().equals(
                                "Risk assessment must be APPROVED "
                                        + "before quote generation. "
                                        + "Current status: PENDING"
                        )
                )
                .verify();

        verify(template, never())
                .insert(Quote.class);
    }

    // =========================================================
    // RISK ID MISMATCH
    // =========================================================

    @Test
    void generateQuotes_ShouldThrowRiskServiceException_WhenRiskApplicationIdDoesNotMatch() {

        GenerateQuoteRequest request =
                createGenerateQuoteRequest();

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(quoteRepository.existsByApplicationId(
                APPLICATION_ID
        )).thenReturn(
                Mono.just(false)
        );

        AtomicInteger callNumber =
                new AtomicInteger();

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenAnswer(invocation -> {

            if (callNumber.incrementAndGet() == 1) {
                return Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                verifiedApplicationJson()
                        )
                );
            }

            return Mono.just(
                    jsonResponse(
                            HttpStatus.OK,
                            """
                            {
                              "assessmentId": "assessment-101",
                              "applicationId": "different-application",
                              "riskScore": 80,
                              "riskCategory": "LOW",
                              "premiumMultiplier": 1.25,
                              "status": "APPROVED"
                            }
                            """
                    )
            );
        });

        StepVerifier.create(
                        policyService.generateQuotes(
                                request
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof RiskServiceException
                                && error.getMessage().equals(
                                "Application ID returned by "
                                        + "Risk Service does not match"
                        )
                )
                .verify();

        verify(template, never())
                .insert(Quote.class);
    }

    // =========================================================
    // BUYER OWNERSHIP SUCCESS
    // =========================================================

    @Test
    void getQuote_ShouldReturnQuote_WhenBuyerOwnsApplication() {

        Quote quote =
                createQuote();

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                verifiedApplicationJson()
                        )
                )
        );

        StepVerifier.create(
                        policyService.getQuote(
                                QUOTE_ID,
                                OWNER_USER_ID,
                                "BUYER"
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
                })
                .verifyComplete();

        verify(jwtService)
                .generateServiceToken();
    }

    // =========================================================
    // BUYER OWNERSHIP FAILURE
    // =========================================================

    @Test
    void getQuote_ShouldThrowAccessDenied_WhenBuyerDoesNotOwnApplication() {

        Quote quote =
                createQuote();

        when(quoteRepository.findById(
                QUOTE_ID
        )).thenReturn(
                Mono.just(quote)
        );

        when(jwtService.generateServiceToken())
                .thenReturn(SERVICE_TOKEN);

        when(exchangeFunction.exchange(
                any(ClientRequest.class)
        )).thenReturn(
                Mono.just(
                        jsonResponse(
                                HttpStatus.OK,
                                """
                                {
                                  "applicationId": "application-101",
                                  "userId": 99,
                                  "propertyValue": 5000000.00,
                                  "status": "VERIFIED"
                                }
                                """
                        )
                )
        );

        StepVerifier.create(
                        policyService.getQuote(
                                QUOTE_ID,
                                OWNER_USER_ID,
                                "BUYER"
                        )
                )
                .expectErrorMatches(error ->
                        error instanceof AccessDeniedException
                                && "You do not own this application"
                                .equals(error.getMessage())
                )
                .verify();

        verify(jwtService)
                .generateServiceToken();

        verify(exchangeFunction)
                .exchange(
                        any(ClientRequest.class)
                );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private ClientResponse jsonResponse(
            HttpStatus status,
            String body
    ) {
        return ClientResponse
                .create(status)
                .header(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .body(body)
                .build();
    }

    private String verifiedApplicationJson() {
        return """
                {
                  "applicationId": "application-101",
                  "userId": 4,
                  "propertyValue": 5000000.00,
                  "status": "VERIFIED"
                }
                """;
    }

    private String submittedApplicationJson() {
        return """
                {
                  "applicationId": "application-101",
                  "userId": 4,
                  "propertyValue": 5000000.00,
                  "status": "SUBMITTED"
                }
                """;
    }

    private String approvedRiskJson() {
        return """
                {
                  "assessmentId": "assessment-101",
                  "applicationId": "application-101",
                  "riskScore": 80,
                  "riskCategory": "LOW",
                  "premiumMultiplier": 1.25,
                  "status": "APPROVED",
                  "assessedBy": "ADMIN"
                }
                """;
    }

    private String pendingRiskJson() {
        return """
                {
                  "assessmentId": "assessment-101",
                  "applicationId": "application-101",
                  "riskScore": 80,
                  "riskCategory": "LOW",
                  "premiumMultiplier": 1.25,
                  "status": "PENDING",
                  "assessedBy": "ADMIN"
                }
                """;
    }

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

    private Quote createQuote() {

        Quote quote =
                new Quote();

        quote.setQuoteId(
                QUOTE_ID
        );

        quote.setApplicationId(
                APPLICATION_ID
        );

        quote.setPlanName(
                "STANDARD"
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

        quote.setStatus(
                QuoteStatus.GENERATED.name()
        );

        quote.setNew(false);

        return quote;
    }

    private Quote findPlan(
            Iterable<Quote> quotes,
            String planName
    ) {
        for (Quote quote : quotes) {
            if (planName.equals(
                    quote.getPlanName()
            )) {
                return quote;
            }
        }

        return null;
    }
}