package com.retailai.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/landing.html",
                                "/mirror.html",
                                "/merchant-dashboard.html",
                                "/merchant-inventory.html",
                                "/merchant-activity.html",
                                "/favicon.ico",
                                "/retailers.js",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/images/products/**",
                                "/health",
                                "/error",
                                "/h2-console/**"
                        ).permitAll()

                        .requestMatchers("/api/v1/saas/auth/signup").permitAll()
                        .requestMatchers("/api/v1/saas/auth/login").permitAll()

                        /*
                         * Public smart-mirror read endpoints.
                         * These are safe for mirror/demo display because they are GET-only.
                         * Write/update/admin actions remain protected below.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/macy-stylist/scan/**",
                                "/api/v1/macy-stylist/look/**",
                                "/api/v1/macy-stylist/associate/inventory"
                        ).permitAll()

                        .requestMatchers("/api/v1/macy-stylist/admin/**").hasRole("OWNER")
                        .requestMatchers("/api/v1/merchant/inventory/**").hasRole("OWNER")

                        .requestMatchers("/api/v1/macy-stylist/**").authenticated()
                        .requestMatchers("/api/**").authenticated()

                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "http://localhost:3000",
                "http://127.0.0.1:8080",
                "http://127.0.0.1:3000"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin"
        ));

        configuration.setExposedHeaders(List.of(
                "Content-Disposition"
        ));

        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}