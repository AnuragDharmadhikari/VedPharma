package org.ved.crm.territory;

import jakarta.validation.constraints.NotBlank;

public record UpdateTerritoryRequest(
        @NotBlank String name,
        @NotBlank String state,
        String zone
) {}