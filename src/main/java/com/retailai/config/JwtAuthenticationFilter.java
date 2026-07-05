package com.retailai.config;

import com.retailai.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
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
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String method = clean(request.getMethod()).toUpperCase();
        String servletPath = normalizePath(request.getServletPath());
        String requestUri = normalizePath(request.getRequestURI());

        if ("OPTIONS".equals(method)) {
            return true;
        }

        return isPublicPath(servletPath) || isPublicPath(requestUri);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String path = request.getRequestURI();
        final String authHeader = request.getHeader("Authorization");

        debug("=== JWT FILTER START ===");
        debug("Path: " + path);
        debug("Authorization header present: " + (authHeader != null));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            debug("No Bearer token found. Continuing without authentication.");
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String token = authHeader.substring(7).trim();

            if (token.isBlank()) {
                debug("Bearer token was blank.");
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

            final String email = clean(jwtService.extractEmail(token));
            final String role = normalizeRole(jwtService.extractRole(token));

            debug("Email from token: " + email);
            debug("Role from token: " + role);
            debug("Existing auth in context: " + SecurityContextHolder.getContext().getAuthentication());

            if (!email.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<SimpleGrantedAuthority> authorities =
                        role.isBlank()
                                ? Collections.emptyList()
                                : List.of(new SimpleGrantedAuthority("ROLE_" + role));

                User principal = new User(email, "", authorities);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                authorities
                        );

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

    private boolean isPublicPath(String path) {
        String safePath = normalizePath(path);

        if (safePath.isBlank()) {
            return true;
        }

        return safePath.equals("/")
                || safePath.equals("/index.html")
                || safePath.equals("/landing.html")
                || safePath.equals("/mirror.html")
                || safePath.equals("/merchant-dashboard.html")
                || safePath.equals("/merchant-inventory.html")
                || safePath.equals("/merchant-activity.html")
                || safePath.equals("/favicon.ico")
                || safePath.equals("/retailers.js")
                || safePath.equals("/health")
                || safePath.equals("/error")

                || safePath.startsWith("/css/")
                || safePath.startsWith("/js/")
                || safePath.startsWith("/images/")
                || safePath.startsWith("/images/products/")
                || safePath.startsWith("/images.products/")
                || safePath.startsWith("/webjars/")
                || safePath.startsWith("/assets/")

                || safePath.startsWith("/h2-console/")

                || safePath.equals("/api/v1/saas/auth/login")
                || safePath.equals("/api/v1/saas/auth/signup");
    }

    private String normalizePath(String value) {
        String cleaned = clean(value);

        if (cleaned.isBlank()) {
            return "";
        }

        int queryIndex = cleaned.indexOf("?");

        if (queryIndex >= 0) {
            cleaned = cleaned.substring(0, queryIndex);
        }

        if (!cleaned.startsWith("/")) {
            cleaned = "/" + cleaned;
        }

        return cleaned;
    }

    private String normalizeRole(String role) {
        String cleaned = clean(role);

        if (cleaned.isBlank()) {
            return "";
        }

        return cleaned
                .replace("ROLE_", "")
                .trim()
                .toUpperCase();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void debug(String message) {
        if (DEBUG_JWT_FILTER) {
            System.out.println(message);
        }
    }
}