package org.ved.crm.user;

import java.time.Instant;
import java.util.UUID;

public record UserDto(
        UUID id,
        String fullName,
        String email,
        Role role,
        String phone,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {}
