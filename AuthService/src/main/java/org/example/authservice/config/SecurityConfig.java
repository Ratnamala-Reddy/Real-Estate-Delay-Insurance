package org.example.authservice.config;

import org.example.authservice.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                .addFilterAt(
                        jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )

                .authorizeExchange(exchange ->

                        exchange

                                .pathMatchers(
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/logout",
                                        "/actuator/**"
                                )
                                .permitAll()

                                .pathMatchers("/admin/**")
                                .hasRole("ADMIN")

                                .pathMatchers("/api/v1/users/**")
                                .authenticated()

                                .anyExchange()
                                .authenticated()
                )

                .build();
}}