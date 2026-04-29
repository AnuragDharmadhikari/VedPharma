package org.ved.crm.stockist;

import org.springframework.stereotype.Component;

@Component
public class StockistMapper {
    public StockistDto toDto(Stockist stockist){
        return new StockistDto(
                stockist.getId(),
                stockist.getAssignedRep().getId(),
                stockist.getAssignedRep().getFullName(),
                stockist.getFirmName(),
                stockist.getOwnerName(),
                stockist.getGstin(),
                stockist.getState(),
                stockist.getCity(),
                stockist.getAddress(),
                stockist.getPhone(),
                stockist.isActive(),
                stockist.getCreatedAt(),
                stockist.getUpdatedAt()
        );
    }
}
