package org.example.applicationservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        String token = resolveToken(exchange);

        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }

        try {
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            Integer userId = jwtService.extractUserId(token);

            if (email == null
                    // If userId is needed globally in your app, we can inject it into details below
                    || role == null
                    || userId == null) {

                return chain.filter(exchange);
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role.toUpperCase()
                                    )
                            )
                    );

            // Set the userId inside the authentication details object if your app needs to read it later
            authentication.setDetails(userId);

            return chain.filter(exchange)
                    .contextWrite(
                            ReactiveSecurityContextHolder
                                    .withAuthentication(authentication)
                    );

        } catch (Exception exception) {
            /*
             * Invalid or expired JWT.
             * Continue without authentication.
             * Spring Security will return 401 for protected endpoints.
             */
            return chain.filter(exchange);
        }
    }

    private String resolveToken(
            ServerWebExchange exchange
    ) {
        HttpCookie cookie =
                exchange.getRequest()
                        .getCookies()
                        .getFirst("accessToken");

        if (cookie != null
                && cookie.getValue() != null
                && !cookie.getValue().isBlank()) {

            return cookie.getValue();
        }

        String authorizationHeader =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null
                || authorizationHeader.isBlank()) {

            return null;
        }

        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        return authorizationHeader;
    }
}
