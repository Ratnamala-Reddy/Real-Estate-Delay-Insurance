package org.example.claimservice.security;

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
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = resolveToken(exchange);

        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }

        try {
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            Integer userId = jwtService.extractUserId(token);

            if (email == null || email.isBlank() ||
                    role == null || role.isBlank() ||
                    userId == null) {
                return chain.filter(exchange);
            }

            String normalizedRole = role.startsWith("ROLE_") ? role.substring(5) : role;
            normalizedRole = normalizedRole.toUpperCase();

            AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                    userId,
                    email,
                    normalizedRole
            );

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
            );

            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));

        } catch (Exception exception) {
            /*
             * Invalid, modified, or expired token.
             * Continue anonymously so protected endpoints return 401.
             */
            return chain.filter(exchange);
        }
    }

    private String resolveToken(ServerWebExchange exchange) {
        // 1. Try to extract token from Cookies
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst("accessToken");
        if (cookie != null && cookie.getValue() != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }

        // 2. Try to extract token from Authorization Header
        String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        return authorizationHeader;
    }
}
