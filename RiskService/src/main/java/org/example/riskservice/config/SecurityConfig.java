package org.example.riskservice.config;


import org.example.riskservice.security.JwtAuthenticationFilter;
import org.example.riskservice.security.JwtService;
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
                                .pathMatchers("/actuator/**")
                                .permitAll()

                                .pathMatchers(
                                        "/api/v1/risk-assessments/**"
                                )
                                .authenticated()

                                .anyExchange()
                                .authenticated()
                )

                .build();
    }
}