package org.ved.crm.returns;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReturnMapper {

    private final CreditNoteMapper creditNoteMapper;

    public ReturnDto toDto(Return returnDoc, CreditNote creditNote) {
        return new ReturnDto(
                returnDoc.getId(),
                returnDoc.getReturnNumber(),
                returnDoc.getChemist() != null ? returnDoc.getChemist().getId() : null,
                returnDoc.getChemist() != null ? returnDoc.getChemist().getFirmName() : null,
                returnDoc.getStockist() != null ? returnDoc.getStockist().getId() : null,
                returnDoc.getStockist() != null ? returnDoc.getStockist().getFirmName() : null,
                returnDoc.getReturnDate(),
                returnDoc.getReason(),
                returnDoc.getStatus(),
                returnDoc.getReturnItems().stream()
                        .map(this::toReturnItemDto)
                        .toList(),
                creditNote != null ? creditNoteMapper.toDto(creditNote) : null,
                returnDoc.getCreatedAt(),
                returnDoc.getUpdatedAt()
        );
    }

    private ReturnItemDto toReturnItemDto(ReturnItem item) {
        return new ReturnItemDto(
                item.getId(),
                item.getBatch().getId(),
                item.getBatch().getBatchNumber(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getHsnCode(),
                item.getQuantity(),
                item.getCondition(),
                item.getUnitPrice(),
                item.getLineTotal()
        );
    }
}