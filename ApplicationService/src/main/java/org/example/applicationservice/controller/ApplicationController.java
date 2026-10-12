package org.example.applicationservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.applicationservice.Service.ApplicationService;
import org.example.applicationservice.dto.ApplicationStatusUpdateRequest;
import org.example.applicationservice.dto.CreateApplicationRequest;
import org.example.applicationservice.dto.UploadDocumentRequest;
import org.example.applicationservice.Entity.Application;
import org.example.applicationservice.Entity.ApplicationDocument;
import org.example.applicationservice.exceptions.ApplicationNotFoundException;
import org.example.applicationservice.security.JwtService;
import org.example.applicationservice.Service.ApplicationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final JwtService jwtService;

    private record CallerIdentity(Integer userId, String role, String email) {}

    private String resolveToken(String cookieToken, String authHeader) {
        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        if (authHeader != null && !authHeader.isBlank()) {
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return authHeader;
        }
        return null;
    }

    private CallerIdentity resolveCaller(
            String userIdHeader,
            String roleHeader,
            String emailHeader,
            String cookieToken,
            String authHeader
    ) {
        String token = resolveToken(
                cookieToken,
                authHeader
        );

        /*
         * If a JWT is available, always use its claims.
         * Client-provided headers cannot override JWT identity.
         */
        if (token != null && !token.isBlank()) {
            Integer userId =
                    jwtService.extractUserId(token);

            String role =
                    jwtService.extractRole(token);

            String email =
                    jwtService.extractEmail(token);

            return new CallerIdentity(
                    userId,
                    role,
                    email
            );
        }

        /*
         * Use headers only when no JWT is available.
         * These headers should come from a trusted API Gateway.
         */
        Integer userId = null;

        if (userIdHeader != null
                && !userIdHeader.isBlank()) {

            try {
                userId = Integer.parseInt(
                        userIdHeader.trim()
                );
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Invalid X-User-Id header"
                );
            }
        }

        String role =
                roleHeader != null
                        && !roleHeader.isBlank()
                        ? roleHeader.trim()
                        : null;

        String email =
                emailHeader != null
                        && !emailHeader.isBlank()
                        ? emailHeader.trim()
                        : null;

        return new CallerIdentity(
                userId,
                role,
                email
        );
    }
    // BUYER ONLY - CREATE
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping
    public Mono<Application> createApplication(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(name = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(name = "X-User-Email", required = false) String emailHeader,
            @CookieValue(name = "accessToken", required = false) String cookieToken,
            @RequestHeader(name = "Authorization", required = false) String authHeader,
            @RequestBody CreateApplicationRequest request
    ) {
        CallerIdentity caller = resolveCaller(userIdHeader, roleHeader, emailHeader, cookieToken, authHeader);
        if (caller.userId() == null) {
            return Mono.error(new IllegalArgumentException("Authenticated user ID is required"));
        }

        return applicationService.createApplication(
                caller.userId(),
                request
        );
    }

    // BUYER (OWNER) OR ADMIN - GET APPLICATION BY ID
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/{applicationId}")
    public Mono<Application> getApplication(
            @PathVariable String applicationId,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(name = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(name = "X-User-Email", required = false) String emailHeader,
            @CookieValue(name = "accessToken", required = false) String cookieToken,
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        CallerIdentity caller = resolveCaller(userIdHeader, roleHeader, emailHeader, cookieToken, authHeader);

        return applicationService.getApplication(applicationId)
                .switchIfEmpty(Mono.error(new ApplicationNotFoundException("Application not found with id: " + applicationId)))
                .flatMap(app -> {
                    if ("BUYER".equalsIgnoreCase(caller.role())) {
                        if (caller.userId() == null || !caller.userId().equals(app.getUserId())) {
                            return Mono.error(new AccessDeniedException("Access Denied: You do not own this application"));
                        }
                    }
                    return Mono.just(app);
                });
    }

    // BUYER ONLY - GET LOGGED IN USER'S APPLICATIONS
    @PreAuthorize("hasRole('BUYER')")

    @GetMapping
    public Flux<Application> getMyApplications(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(name = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(name = "X-User-Email", required = false) String emailHeader,
            @CookieValue(name = "accessToken", required = false) String cookieToken,
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        CallerIdentity caller = resolveCaller(userIdHeader, roleHeader, emailHeader, cookieToken, authHeader);
        if (caller.userId() == null) {
            return Flux.error(new AccessDeniedException("Access Denied: User not authenticated"));
        }

        return applicationService.getMyApplications(caller.userId());
    }

    // ADMIN ONLY - GET ALL
    @PreAuthorize("hasRole('ADMIN')")

    @GetMapping("/all")
    public Flux<Application> getAllApplications() {
        return applicationService.getAllApplications();
    }

    // ADMIN OR SYSTEM - UPDATE APPLICATION STATUS
    @PreAuthorize("hasRole('ADMIN')")

    @PatchMapping("/{applicationId}/status")
    public Mono<Application> updateApplicationStatus(
            @PathVariable String applicationId,
            @RequestBody ApplicationStatusUpdateRequest request
    ) {
        return applicationService.updateApplicationStatus(
                applicationId,
                request.getStatus()
        );
    }

    @PreAuthorize("hasRole('BUYER')")
    @PutMapping("/{applicationId}")
    public Mono<Application> updateApplication(
            @PathVariable String applicationId,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(name = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(name = "X-User-Email", required = false) String emailHeader,
            @CookieValue(name = "accessToken", required = false) String cookieToken,
            @RequestHeader(name = "Authorization", required = false) String authHeader,
            @RequestBody CreateApplicationRequest request
    ) {
        CallerIdentity caller = resolveCaller(userIdHeader, roleHeader, emailHeader, cookieToken, authHeader);

        return applicationService.getApplication(applicationId)
                .switchIfEmpty(Mono.error(new ApplicationNotFoundException("Application not found with id: " + applicationId)))
                .flatMap(app -> {
                    if (caller.userId() == null
                            || !caller.userId().equals(app.getUserId())) {

                        return Mono.error(
                                new AccessDeniedException(
                                        "Access Denied: You do not own this application"
                                )
                        );
                    }
                    return applicationService.updateApplication(
                            applicationId,
                            request
                    );
                });
    }

    // BUYER (OWNER) OR ADMIN - GET DOCUMENTS
    @PreAuthorize("hasAnyRole('BUYER', 'ADMIN')")
    @GetMapping("/{applicationId}/documents")
    public Flux<ApplicationDocument> getDocuments(
            @PathVariable String applicationId,
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(name = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(name = "X-User-Email", required = false) String emailHeader,
            @CookieValue(name = "accessToken", required = false) String cookieToken,
            @RequestHeader(name = "Authorization", required = false) String authHeader
    ) {
        CallerIdentity caller = resolveCaller(userIdHeader, roleHeader, emailHeader, cookieToken, authHeader);

        return applicationService.getApplication(applicationId)
                .switchIfEmpty(Mono.error(new ApplicationNotFoundException("Application not found with id: " + applicationId)))
                .flatMapMany(app -> {
                    if ("BUYER".equalsIgnoreCase(caller.role()) && (caller.userId() == null || !caller.userId().equals(app.getUserId()))) {
                        return Flux.error(new AccessDeniedException("Access Denied: You do not own this application"));
                    }
                    return applicationService.getDocuments(applicationId);
                });
    }


    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/{applicationId}/documents")
    public Mono<ApplicationDocument> uploadDocument(
            @PathVariable String applicationId,
            @RequestHeader(
                    name = "X-User-Id",
                    required = false
            ) String userIdHeader,
            @RequestHeader(
                    name = "X-User-Role",
                    required = false
            ) String roleHeader,
            @RequestHeader(
                    name = "X-User-Email",
                    required = false
            ) String emailHeader,
            @CookieValue(
                    name = "accessToken",
                    required = false
            ) String cookieToken,
            @RequestHeader(
                    name = "Authorization",
                    required = false
            ) String authHeader,
            @RequestBody UploadDocumentRequest request
    ) {
        CallerIdentity caller = resolveCaller(
                userIdHeader,
                roleHeader,
                emailHeader,
                cookieToken,
                authHeader
        );

        return applicationService.getApplication(applicationId)
                .switchIfEmpty(
                        Mono.error(
                                new ApplicationNotFoundException(
                                        "Application not found with id: "
                                                + applicationId
                                )
                        )
                )
                .flatMap(application -> {
                    if (caller.userId() == null
                            || !caller.userId()
                            .equals(application.getUserId())) {

                        return Mono.error(
                                new AccessDeniedException(
                                        "Access Denied: You do not own this application"
                                )
                        );
                    }

                    return applicationService.uploadDocument(
                            applicationId,
                            request
                    );
                });
    }

    // BUYER ONLY - SUBMIT APPLICATION
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/{applicationId}/submit")
    public Mono<Application> submitApplication(
            @PathVariable String applicationId,
            @RequestHeader(
                    name = "X-User-Id",
                    required = false
            ) String userIdHeader,
            @RequestHeader(
                    name = "X-User-Role",
                    required = false
            ) String roleHeader,
            @RequestHeader(
                    name = "X-User-Email",
                    required = false
            ) String emailHeader,
            @CookieValue(
                    name = "accessToken",
                    required = false
            ) String cookieToken,
            @RequestHeader(
                    name = "Authorization",
                    required = false
            ) String authHeader
    ) {
        CallerIdentity caller = resolveCaller(
                userIdHeader,
                roleHeader,
                emailHeader,
                cookieToken,
                authHeader
        );

        return applicationService.getApplication(applicationId)
                .switchIfEmpty(
                        Mono.error(
                                new ApplicationNotFoundException(
                                        "Application not found with id: "
                                                + applicationId
                                )
                        )
                )
                .flatMap(application -> {
                    if (caller.userId() == null
                            || !caller.userId()
                            .equals(application.getUserId())) {

                        return Mono.error(
                                new AccessDeniedException(
                                        "Access Denied: You do not own this application"
                                )
                        );
                    }

                    return applicationService.submitApplication(
                            applicationId
                    );
                });
    }
    // BUYER ONLY - WITHDRAW
    // BUYER ONLY - WITHDRAW APPLICATION
    @PreAuthorize("hasRole('BUYER')")
    @PostMapping("/{applicationId}/withdraw")
    public Mono<Application> withdrawApplication(
            @PathVariable String applicationId,
            @RequestHeader(
                    name = "X-User-Id",
                    required = false
            ) String userIdHeader,
            @RequestHeader(
                    name = "X-User-Role",
                    required = false
            ) String roleHeader,
            @RequestHeader(
                    name = "X-User-Email",
                    required = false
            ) String emailHeader,
            @CookieValue(
                    name = "accessToken",
                    required = false
            ) String cookieToken,
            @RequestHeader(
                    name = "Authorization",
                    required = false
            ) String authHeader
    ) {
        CallerIdentity caller = resolveCaller(
                userIdHeader,
                roleHeader,
                emailHeader,
                cookieToken,
                authHeader
        );

        return applicationService.getApplication(applicationId)
                .switchIfEmpty(
                        Mono.error(
                                new ApplicationNotFoundException(
                                        "Application not found with id: "
                                                + applicationId
                                )
                        )
                )
                .flatMap(application -> {
                    if (caller.userId() == null
                            || !caller.userId()
                            .equals(application.getUserId())) {

                        return Mono.error(
                                new AccessDeniedException(
                                        "Access Denied: You do not own this application"
                                )
                        );
                    }

                    return applicationService.withdrawApplication(
                            applicationId
                    );
                });
    }
}