package org.ved.crm.chemist;

import org.springframework.stereotype.Component;

@Component
public class ChemistMapper {

    public ChemistDto toDto(Chemist chemist){
        return new ChemistDto(
                chemist.getId(),
                chemist.getAssignedRep().getId(),
                chemist.getAssignedRep().getFullName(),
                chemist.getFirmName(),
                chemist.getOwnerName(),
                chemist.getDrugLicenseNumber(),
                chemist.getGstin(),
                chemist.getState(),
                chemist.getCity(),
                chemist.getAddress(),
                chemist.getPhone(),
                chemist.getEmail(),
                chemist.isActive(),
                chemist.getCreatedAt(),
                chemist.getUpdatedAt()
        );
    }
}
