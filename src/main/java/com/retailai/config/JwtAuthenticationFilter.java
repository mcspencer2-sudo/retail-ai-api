package com.retailai.config;

import com.retailai.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final boolean DEBUG_JWT_FILTER = false;

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String path = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");

        debug("=== JWT FILTER START ===");
        debug("Path: " + path);
        debug("Authorization header present: " + (authHeader != null));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            debug("No Bearer token found.");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String token = authHeader.substring(7).trim();
            debug("Token present after trim: " + !token.isBlank());

            if (token.isBlank()) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            boolean valid = jwtService.isTokenValid(token);
            debug("Token valid: " + valid);

            if (!valid) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            final String email = jwtService.extractEmail(token);
            final String role = jwtService.extractRole(token);

            debug("Email from token: " + email);
            debug("Role from token: " + role);
            debug("Existing auth in context: " + SecurityContextHolder.getContext().getAuthentication());

            if (email != null
                    && !email.isBlank()
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                String normalizedRole = role == null ? "" : role.trim().replace("ROLE_", "");

                List<SimpleGrantedAuthority> authorities =
                        normalizedRole.isBlank()
                                ? Collections.emptyList()
                                : List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole));

                User principal = new User(email, "", authorities);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, authorities);

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);

                debug("Authentication set in SecurityContext.");
                debug("Principal: " + authentication.getPrincipal());
                debug("Authorities: " + authentication.getAuthorities());
                debug("isAuthenticated: " + authentication.isAuthenticated());
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            debug("JWT authentication failed: " + ex.getMessage());

            if (DEBUG_JWT_FILTER) {
                ex.printStackTrace();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void debug(String message) {
        if (DEBUG_JWT_FILTER) {
            System.out.println(message);
        }
    }
}