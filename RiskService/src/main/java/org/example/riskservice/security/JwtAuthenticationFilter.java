package org.example.riskservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        String token =
                resolveToken(exchange);

        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }

        try {
            String email =
                    jwtService.extractEmail(token);

            String role =
                    jwtService.extractRole(token);

            Integer userId =
                    jwtService.extractUserId(token);

            if (email == null
                    || email.isBlank()
                    || role == null
                    || role.isBlank()
                    || userId == null) {

                return chain.filter(exchange);
            }

            String authority =
                    role.startsWith("ROLE_")
                            ? role.toUpperCase()
                            : "ROLE_" + role.toUpperCase();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            authority
                                    )
                            )
                    );

            return chain
                    .filter(exchange)
                    .contextWrite(
                            ReactiveSecurityContextHolder
                                    .withAuthentication(
                                            authentication
                                    )
                    );

        } catch (Exception exception) {
            /*
             * Invalid, expired, or modified token.
             * Continue anonymously. Spring Security returns
             * 401 for protected endpoints.
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
                        .getFirst(
                                HttpHeaders.AUTHORIZATION
                        );

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