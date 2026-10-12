package org.example.apigateway.filter;

import lombok.RequiredArgsConstructor;
import org.example.apigateway.security.JwtService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/payment-webhooks")) {
            return chain.filter(exchange);
        }

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
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            Integer userId = jwtService.extractUserId(token);

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(r -> {
                        r.headers(h -> {
                            h.set("X-User-Email", email != null ? email : "");
                            h.set("X-User-Role", role != null ? role : "");
                            if (userId != null) {
                                h.set("X-User-Id", userId.toString());
                            } else {
                                h.remove("X-User-Id");
                            }
                        });
                    })
                    .build();

            return chain.filter(mutatedExchange);

        } catch (Exception ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}