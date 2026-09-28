package com.autoconsultancy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds production security headers to every response.
 *
 * <ul>
 *   <li>X-Content-Type-Options — prevents MIME sniffing attacks</li>
 *   <li>X-Frame-Options — prevents clickjacking</li>
 *   <li>Referrer-Policy — reduces referrer leakage</li>
 *   <li>Content-Security-Policy — restricts resource origins (API-only, no HTML served)</li>
 *   <li>Permissions-Policy — disables unneeded browser features</li>
 *   <li>Strict-Transport-Security — enables HSTS (effective once served over HTTPS)</li>
 * </ul>
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Prevent MIME-type confusion attacks
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent the API from being embedded in iframes
        response.setHeader("X-Frame-Options", "DENY");

        // Limit referrer information sent to other origins
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // CSP: This is a pure JSON API — no HTML/scripts served from this origin
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; frame-ancestors 'none'");

        // Disable unnecessary browser features
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // HSTS: enforce HTTPS for 1 year once deployed over TLS
        response.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains");

        // Remove server version disclosure
        response.setHeader("X-Powered-By", "");

        filterChain.doFilter(request, response);
    }
}
