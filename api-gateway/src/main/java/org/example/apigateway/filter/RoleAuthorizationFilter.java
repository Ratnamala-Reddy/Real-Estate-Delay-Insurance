package org.example.apigateway.filter;

import lombok.RequiredArgsConstructor;
import org.example.apigateway.security.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RoleAuthorizationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        // 1. Public APIs
        if (path.startsWith("/api/v1/auth")) {
            return chain.filter(exchange);
        }

        // 2. Extract Token
        String token = null;
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("accessToken");
        if (cookie != null && !cookie.getValue().isBlank()) {
            token = cookie.getValue();
        } else {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null) {
            return unauthorized(exchange);
        }

        String role;
        try {
            role = jwtService.extractRole(token);
        } catch (Exception e) {
            return unauthorized(exchange);
        }

        if (role == null) {
            return unauthorized(exchange);
        }

        // 3. ADMIN ONLY endpoints
        boolean isAdminOnly =
                path.startsWith("/admin")
                        || path.equals("/api/v1/applications/all")
                        || path.equals("/api/v1/verifications/applications")
                        || path.contains("/property-verification")
                        || path.startsWith("/api/v1/payment-webhooks")
                        || path.contains("/request-information")
                        || (path.startsWith("/api/v1/risk-assessments") && (
                                path.equals("/api/v1/risk-assessments")
                                        || path.endsWith("/approve")
                                        || path.endsWith("/decline")
                        ))
                        || ((path.startsWith("/api/v1/quotes/applications/") || path.startsWith("/quotes/applications/"))
                                && HttpMethod.POST.equals(method))
                        || ((path.startsWith("/api/v1/policies") || path.startsWith("/policies"))
                                && HttpMethod.POST.equals(method))
                        || (path.startsWith("/api/v1/applications/") && path.endsWith("/status") && HttpMethod.PATCH.equals(method))
                        || path.endsWith("/complete")
                        || path.endsWith("/close")
                        || path.endsWith("/delay-calculation")
                        || path.endsWith("/compensation-calculation")
                        || path.endsWith("/insurance-verification")
                        || (path.startsWith("/api/v1/claims/") && (
                                path.endsWith("/approve")
                                        || path.endsWith("/reject")
                                        || path.endsWith("/close")
                        ));

        if (isAdminOnly) {
            if (!"ADMIN".equalsIgnoreCase(role)) {
                return forbidden(exchange);
            }
            return chain.filter(exchange);
        }

        // 4. BUYER ONLY endpoints
        boolean isBuyerOnly =
                ((path.equals("/api/v1/applications") || path.equals("/api/v1/applications/")) && HttpMethod.POST.equals(method))
                        || ((path.equals("/api/v1/applications") || path.equals("/api/v1/applications/")) && HttpMethod.GET.equals(method))
                        || (path.startsWith("/api/v1/applications/") && HttpMethod.PUT.equals(method))
                        || (path.startsWith("/api/v1/applications/") && path.endsWith("/documents") && HttpMethod.POST.equals(method))
                        || (path.startsWith("/api/v1/applications/") && (path.endsWith("/submit") || path.endsWith("/withdraw")))
                        || (path.endsWith("/accept") && HttpMethod.POST.equals(method))
                        || (path.endsWith("/decline") && HttpMethod.POST.equals(method))
                        || (path.startsWith("/api/v1/premium-payments") && HttpMethod.POST.equals(method))
                        || (path.equals("/api/v1/claims") && HttpMethod.POST.equals(method));

        if (isBuyerOnly) {
            if (!"BUYER".equalsIgnoreCase(role)) {
                return forbidden(exchange);
            }
            return chain.filter(exchange);
        }

        // 5. Shared endpoints (Buyer or Admin) - verified down to ownership in downstream services
        return chain.filter(exchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}