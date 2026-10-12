package org.example.authservice.filter;

import org.example.authservice.security.JwtService;
import org.springframework.http.HttpCookie;
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
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {

        HttpCookie cookie =
                exchange.getRequest()
                        .getCookies()
                        .getFirst("accessToken");

        if (cookie == null) {
            return chain.filter(exchange);
        }

        try {

            String token = cookie.getValue();

            String email = jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);

            System.out.println("========== JWT FILTER ==========");
            System.out.println("Email = " + email);
            System.out.println("Role = " + role);
            System.out.println("================================");

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role
                                    )
                            )
                    );

            return chain.filter(exchange)
                    .contextWrite(
                            ReactiveSecurityContextHolder
                                    .withAuthentication(authentication)
                    );

        } catch (Exception ex) {

            return chain.filter(exchange);
        }
    }
}