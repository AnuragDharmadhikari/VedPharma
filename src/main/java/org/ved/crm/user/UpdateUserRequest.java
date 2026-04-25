package org.ved.crm.user;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 2, message = "Full name must be at least 2 characters")
        String fullName,
        String phone
) {

}
