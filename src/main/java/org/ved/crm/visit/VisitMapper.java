package org.ved.crm.visit;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VisitMapper {

    public VisitDto toDto(Visit visit){
        List<VisitProductDto> productDtos = visit.getVisitProducts()
                .stream()
                .map(this::toVisitProductDto)
                .toList();

        return new VisitDto(
                visit.getId(),
                visit.getRep().getId(),
                visit.getRep().getFullName(),
                visit.getDoctor().getId(),
                visit.getDoctor().getFullName(),
                visit.getDoctor().getSpecialty(),
                visit.getVisitDate(),
                visit.getStatus(),
                visit.getNotes(),
                visit.getAiSummary(),
                productDtos,
                visit.getCreatedAt(),
                visit.getUpdatedAt()

        );
    }

    private VisitProductDto toVisitProductDto(VisitProduct vp) {
        return new VisitProductDto(
                vp.getId(),
                vp.getProduct().getId(),
                vp.getProduct().getName(),
                vp.getProduct().getHsnCode(),
                vp.getSamplesGiven(),
                vp.getFeedback()
        );
    }
}
