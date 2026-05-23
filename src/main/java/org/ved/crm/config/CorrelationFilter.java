package org.ved.crm.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// OncePerRequestFilter — guaranteed to run exactly once per HTTP request
// @Order(1) — runs before all other filters including JwtAuthFilter
// This ensures every log line has a correlationId from the very start
@Component
@Order(1)
public class CorrelationFilter extends OncePerRequestFilter {

    // The key used to store correlationId in MDC and response header
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Check if caller sent a correlationId (e.g. from API gateway)
            // If not, generate a new one — first 8 chars of a UUID is enough
            // Full UUID is overkill and clutters logs
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString().substring(0, 8);
            }

            // MDC = Mapped Diagnostic Context — a thread-local map
            // Logback reads from MDC automatically when formatting log lines
            // Every log.info/warn/error on this thread will include correlationId
            MDC.put(CORRELATION_ID_KEY, correlationId);

            // Add correlationId to response header so frontend/API clients
            // can reference it when reporting bugs — "my request ID was a3f8c2d1"
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue the filter chain — request proceeds normally
            filterChain.doFilter(request, response);

        } finally {
            // CRITICAL — always clear MDC after request completes
            // Tomcat reuses threads — if we don't clear, the next request
            // on this thread will inherit the previous request's correlationId
            MDC.clear();
        }
    }
}