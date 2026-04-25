package org.ved.crm.doctor;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.territory.Territory;
import org.ved.crm.territory.TerritoryRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorMapper doctorMapper;
    private final TerritoryRepository territoryRepository;

    public List<DoctorDto> getAllActiveDoctors(){
        return doctorRepository.findByIsActiveTrue()
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    public DoctorDto getDoctorById(UUID id){
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Doctor","id",id));
        return doctorMapper.toDto(doctor);

    }

    public List<DoctorDto> getDoctorsByTerritory(UUID territoryId){
        return doctorRepository.findByTerritoryId(territoryId)
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    public List<DoctorDto> getDoctorsBySpecialty(String specialty){
        return doctorRepository.findBySpecialty(specialty)
                .stream()
                .map(doctorMapper::toDto)
                .toList();
    }

    @Transactional
    public DoctorDto createDoctor(CreateDoctorRequest request){
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

        if(request.territoryId() != null){
            Territory territory = territoryRepository
                    .findById(request.territoryId())
                    .orElseThrow(()->new ResourceNotFoundException("Territory","id",request.territoryId()));
            doctor.setTerritory(territory);
        }

        Doctor saved = doctorRepository.save(doctor);
        return doctorMapper.toDto(saved);
    }

    @Transactional
    public DoctorDto updateDoctor(UUID id, UpdateDoctorRequest request) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor", "id", id));

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

        Doctor saved = doctorRepository.save(doctor);
        return doctorMapper.toDto(saved);
    }

    @Transactional
    public void deactivateDoctor(UUID id){
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Doctor","id",id));
        doctor.setActive(false);
    }
}
