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
        String path = request.getServletPath();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        String safePath = path == null ? "" : path.trim();
        String safeUri = uri == null ? "" : uri.trim();

        return "OPTIONS".equalsIgnoreCase(method)
                || isPublicPath(safePath)
                || isPublicPath(safeUri);
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
            debug("No Bearer token found.");
            SecurityContextHolder.clearContext();
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

            final String email = clean(jwtService.extractEmail(token));
            final String role = clean(jwtService.extractRole(token));

            debug("Email from token: " + email);
            debug("Role from token: " + role);
            debug("Existing auth in context: " + SecurityContextHolder.getContext().getAuthentication());

            if (!email.isBlank()
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                String normalizedRole = normalizeRole(role);

                List<SimpleGrantedAuthority> authorities =
                        normalizedRole.isBlank()
                                ? Collections.emptyList()
                                : List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole));

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
        if (path == null || path.isBlank()) {
            return true;
        }

        return path.equals("/")
                || path.equals("/index.html")
                || path.equals("/favicon.ico")
                || path.equals("/retailers.js")
                || path.equals("/health")
                || path.equals("/error")
                || path.startsWith("/h2-console")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/webjars/")
                || path.equals("/api/v1/saas/auth/login")
                || path.equals("/api/v1/saas/auth/signup");
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