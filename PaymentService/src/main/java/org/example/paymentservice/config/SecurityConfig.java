package org.example.paymentservice.config;

import org.example.paymentservice.security.JwtAuthenticationFilter;
import org.example.paymentservice.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService
    ) {
        return new JwtAuthenticationFilter(
                jwtService
        );
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        return http
                .csrf(
                        ServerHttpSecurity.CsrfSpec::disable
                )

                .httpBasic(
                        ServerHttpSecurity.HttpBasicSpec::disable
                )

                .formLogin(
                        ServerHttpSecurity.FormLoginSpec::disable
                )

                .exceptionHandling(exceptionHandling ->
                        exceptionHandling

                                .authenticationEntryPoint(
                                        (exchange, exception) -> {
                                            exchange.getResponse()
                                                    .setStatusCode(
                                                            HttpStatus.UNAUTHORIZED
                                                    );

                                            return exchange
                                                    .getResponse()
                                                    .setComplete();
                                        }
                                )

                                .accessDeniedHandler(
                                        (exchange, exception) -> {
                                            exchange.getResponse()
                                                    .setStatusCode(
                                                            HttpStatus.FORBIDDEN
                                                    );

                                            return exchange
                                                    .getResponse()
                                                    .setComplete();
                                        }
                                )
                )

                .addFilterAt(
                        jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )

                .authorizeExchange(exchange ->
                        exchange

                                // Public monitoring endpoints
                                .pathMatchers("/actuator/**")
                                .permitAll()

                                // All payment APIs require authentication.
                                // Endpoint-specific roles are enforced by
                                // @PreAuthorize in the controllers.
                                .pathMatchers(
                                        "/api/v1/premium-payments/**",
                                        "/api/v1/compensation-payments/**",
                                        "/api/v1/payment-webhooks/**"
                                )
                                .authenticated()

                                // Protect every other endpoint by default
                                .anyExchange()
                                .authenticated()
                )

                .build();
    }
}