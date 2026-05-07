package org.ved.crm.doctor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ved.crm.audit.Audited;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.territory.Territory;
import org.ved.crm.territory.TerritoryRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorMapper doctorMapper;
    private final TerritoryRepository territoryRepository;

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<DoctorDto> getAllActiveDoctors() {
        return doctorRepository.findByIsActiveTrue()
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public DoctorDto getDoctorById(UUID id) {
        return doctorRepository.findByIdWithDetails(id)
                .map(doctorMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor", "id", id));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<DoctorDto> getDoctorsByTerritory(UUID territoryId) {
        return doctorRepository.findByTerritoryId(territoryId)
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<DoctorDto> getDoctorsBySpecialty(String specialty) {
        return doctorRepository.findBySpecialty(specialty)
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    @Transactional
    public DoctorDto createDoctor(CreateDoctorRequest request) {
        Doctor doctor = Doctor.builder()
                .fullName(request.fullName())
                .specialty(request.specialty())
                .hospitalName(request.hospitalName())
                .tier(request.tier())
                .phone(request.phone())
                .email(request.email())
                .city(request.city())
                .state(request.state())
                .build();

        if (request.territoryId() != null) {
            Territory territory = territoryRepository
                    .findById(request.territoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Territory", "id", request.territoryId()));
            doctor.setTerritory(territory);
        }

        doctorRepository.save(doctor);
        return doctorMapper.toDto(
                doctorRepository.findByIdWithDetails(
                        doctor.getId()).orElseThrow());
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    @Transactional
    public DoctorDto updateDoctor(UUID id, UpdateDoctorRequest request) {
        Doctor doctor = doctorRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor", "id", id));

        if (!doctor.isActive() && (request.isActive() == null || !request.isActive())) {
            throw new IllegalArgumentException(
                    "Cannot update a deactivated doctor: " + doctor.getFullName()
                            + ". Set isActive to true to reactivate first.");
        }

        doctor.setFullName(request.fullName());
        doctor.setSpecialty(request.specialty());
        doctor.setHospitalName(request.hospitalName());
        doctor.setTier(request.tier());
        doctor.setPhone(request.phone());
        doctor.setEmail(request.email());
        doctor.setCity(request.city());
        doctor.setState(request.state());

        if (request.territoryId() != null) {
            Territory territory = territoryRepository
                    .findById(request.territoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Territory", "id", request.territoryId()));
            doctor.setTerritory(territory);
        }

        if (request.isActive() != null) {
            doctor.setActive(request.isActive());
        }

        doctorRepository.save(doctor);
        return doctorMapper.toDto(
                doctorRepository.findByIdWithDetails(id).orElseThrow());
    }

    @Audited(action = "DOCTOR_DEACTIVATED", entityType = "Doctor")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public void deactivateDoctor(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor", "id", id));
        doctor.setActive(false);
        doctorRepository.save(doctor);
    }
}