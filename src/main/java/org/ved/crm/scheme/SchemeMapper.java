package org.ved.crm.scheme;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SchemeMapper {

    public SchemeDto toDto(Scheme scheme){

        UUID chemistId = scheme.getChemist() != null ? scheme.getChemist().getId() : null;
        String chemistFirmName = scheme.getChemist() != null ? scheme.getChemist().getFirmName() : null;
        UUID stockistId = scheme.getStockist() != null ? scheme.getStockist().getId() : null;
        String stockistFirmName = scheme.getStockist() != null ? scheme.getStockist().getFirmName() : null;

        return new SchemeDto(
                scheme.getId(),

                scheme.getProduct().getId(),
                scheme.getProduct().getName(),
                scheme.getProduct().getMolecule(),

                chemistId,
                chemistFirmName,

                stockistId,
                stockistFirmName,

                scheme.getSchemeType(),
                scheme.getMinQuantity(),

                scheme.getFreeQuantity(),
                scheme.getDiscountPct(),

                scheme.getValidFrom(),
                scheme.getValidTo(),
                scheme.isActive(),

                scheme.getCreatedAt(),
                scheme.getUpdatedAt()
        );
    }

}
