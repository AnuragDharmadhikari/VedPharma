package org.ved.crm.auth;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.audit.Audited;
import org.ved.crm.security.JwtService;
import org.ved.crm.security.LoginRateLimiter;
import org.ved.crm.user.Role;
import org.ved.crm.user.User;
import org.ved.crm.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LoginRateLimiter loginRateLimiter;

    @Value("${application.jwt.expiration-ms}")
    private long expirationMs;

    @Audited(action = "USER_REGISTERED", entityType = "User")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(
                    "Email already registered: " + request.email()
            );
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role() != null ? request.role() : Role.REP)
                .phone(request.phone())
                .build();

        userRepository.save(user);

        var userDetails = org.springframework.security.core.userdetails
                .User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .build();

        String token = jwtService.generateToken(userDetails);
        return AuthResponse.of(token, expirationMs);
    }

    public AuthResponse login(LoginRequest request) {

        // Step 1 — Check rate limit BEFORE attempting authentication
        // Throws TooManyRequestsException if limit exceeded
        loginRateLimiter.checkRateLimit(request.email());

        try {
            // Step 2 — Attempt authentication
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );

            // Step 3 — Load user
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow();

            // Step 4 — Check if account is active
            if (!user.isActive()) {
                throw new BadCredentialsException(
                        "Account is deactivated. Please contact your administrator.");
            }

            // Step 5 — Success — clear any previous failed attempts
            loginRateLimiter.clearFailedAttempts(request.email());

            var userDetails = org.springframework.security.core.userdetails
                    .User.builder()
                    .username(user.getEmail())
                    .password(user.getPasswordHash())
                    .roles(user.getRole().name())
                    .build();

            String token = jwtService.generateToken(userDetails);
            return AuthResponse.of(token, expirationMs);

        } catch (BadCredentialsException ex) {

            // Step 6 — Failed authentication — record the attempt
            // Only record for BadCredentialsException — wrong password
            // Don't record for TooManyRequestsException — already rate limited
            loginRateLimiter.recordFailedAttempt(request.email());
            throw ex;
        }
    }

}
