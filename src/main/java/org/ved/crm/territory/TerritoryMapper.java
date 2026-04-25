package org.ved.crm.territory;

import org.springframework.stereotype.Component;

@Component
public class TerritoryMapper {

    public TerritoryDto toDto(Territory territory) {
        return new TerritoryDto(
                territory.getId(),
                territory.getName(),
                territory.getState(),
                territory.getZone(),
                territory.isActive(),
                territory.getCreatedAt(),
                territory.getUpdatedAt()
        );
    }
}
