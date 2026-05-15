package org.ved.crm.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No token present — let the request through unauthenticated.
        // Spring Security will block it at the authorization layer if the
        // endpoint requires authentication. This is correct behaviour for
        // public endpoints like /auth/login.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            // extractUsername calls jwtService.extractAllClaims() internally.
            // If the token is expired, JJWT throws ExpiredJwtException here —
            // before we even reach isTokenValid(). That's why the old code
            // was crashing and returning 500: the exception was unhandled and
            // bubbled all the way up to Tomcat.
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            // Token was valid — continue the filter chain normally.
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // Token is syntactically valid JWT but the expiry time has passed.
            // We short-circuit here — do NOT call filterChain.doFilter().
            // Writing directly to the response bypasses Spring MVC entirely,
            // so this returns immediately as a clean 401 JSON response.
            // The Axios interceptor on the frontend will catch this 401
            // and redirect the user to /login.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"message\":\"Token expired. Please log in again.\"}"
            );

        } catch (JwtException e) {
            // Covers malformed tokens, invalid signatures, unsupported JWT, etc.
            // Same pattern — short-circuit with a 401.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"message\":\"Invalid token.\"}"
            );
        }
    }
}