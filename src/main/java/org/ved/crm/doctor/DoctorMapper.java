package org.ved.crm.doctor;

import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

    public DoctorDto toDto(Doctor doctor) {
        return new DoctorDto(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getSpecialty(),
                doctor.getHospitalName(),
                doctor.getTier(),
                doctor.getPhone(),
                doctor.getEmail(),
                doctor.getCity(),
                doctor.getState(),
                doctor.getTerritory() != null ? doctor.getTerritory().getId() : null,
                doctor.getTerritory() != null ? doctor.getTerritory().getName() : null,
                doctor.isActive(),
                doctor.getCreatedAt(),
                doctor.getUpdatedAt()
        );
    }
}
