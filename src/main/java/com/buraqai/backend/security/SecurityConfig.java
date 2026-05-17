package com.buraqai.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.List;
import org.springframework.security.authentication.DisabledException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← enables @PreAuthorize annotations on controllers
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth

                        // Public routes — no token needed
                        .requestMatchers("/api/health", "/api/auth/**",  "/api/documents/*/status", "/error").permitAll()

                        // Employee or higher
                        .requestMatchers("/api/employee/**").hasAnyRole("EMPLOYEE", "SUPPORT_AGENT", "ADMIN", "SYSTEM_ADMIN")

                        // Support Agent or higher
                        .requestMatchers("/api/tickets/**").hasAnyRole("EMPLOYEE", "SUPPORT_AGENT", "ADMIN", "SYSTEM_ADMIN")

                        // Admin or System Admin only
                        .requestMatchers("/api/documents/**").hasAnyRole("ADMIN", "SYSTEM_ADMIN")

                        // System Admin only
                        .requestMatchers("/api/admin/**").hasRole("SYSTEM_ADMIN")
                        .requestMatchers("/api/actuator/**").hasRole("SYSTEM_ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");

                            if (authException.getCause() instanceof DisabledException) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                new ObjectMapper().writeValue(response.getOutputStream(), Map.of(
                                        "status", 403,
                                        "message", "Your account has been deactivated. Please contact your system administrator."
                                ));
                            } else {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                new ObjectMapper().writeValue(response.getOutputStream(), Map.of(
                                        "status", 401,
                                        "message", "Unauthorized"
                                ));
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            new ObjectMapper().writeValue(response.getOutputStream(), Map.of(
                                    "status", 403,
                                    "message", "Forbidden"
                            ));
                        })
                )

                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        return new UrlBasedCorsConfigurationSource() {{
            registerCorsConfiguration("/**", configuration);
        }};
    }
}