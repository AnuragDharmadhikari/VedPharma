package org.ved.crm.chemist;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.common.exception.ResourceNotFoundException;
import org.ved.crm.user.User;
import org.ved.crm.user.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChemistService {

    private final ChemistRepository chemistRepository;
    private final UserRepository userRepository;
    private final ChemistMapper chemistMapper;

    // GET all active chemists
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<ChemistDto> getAllChemists(){
        return chemistRepository.findAllActiveWithDetails()
                .stream()
                .map(chemistMapper::toDto)
                .toList();
    }

    // GET chemist by ID
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public ChemistDto getChemistById(UUID id){
        Chemist chemist = chemistRepository.findByIdWithDetails(id)
                .orElseThrow(()->new ResourceNotFoundException("Chemist","id",id));
        return chemistMapper.toDto(chemist);
    }

    // GET chemists by rep
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<ChemistDto> getChemistByRep(UUID repID){
        return chemistRepository.findByAssignedRepId(repID)
                .stream()
                .map(chemistMapper::toDto)
                .toList();
    }

    //CREATE Chemist
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    @Transactional
    public ChemistDto createChemist(CreateChemistRequest request){

        User rep = userRepository.findById(request.assignedRepId())
                .orElseThrow(()->new ResourceNotFoundException("User","id",request.assignedRepId()));

        if (!rep.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot assign chemist to deactivated rep: " + rep.getFullName());
        }

        if(chemistRepository.existsByDrugLicenseNumber(request.drugLicenseNumber())){
            throw new IllegalArgumentException("Chemist with Drug License Number already exists: "+request.drugLicenseNumber());
        }

        if(request.gstin()!=null && !request.gstin().isBlank()){
            if(chemistRepository.existsByGstin(request.gstin())){
                throw new IllegalArgumentException("Chemist with GSTIN already exists: " + request.gstin());
            }
        }

        Chemist chemist = Chemist.builder()
                .assignedRep(rep)
                .firmName(request.firmName())
                .ownerName(request.ownerName())
                .drugLicenseNumber(request.drugLicenseNumber())
                .gstin(request.gstin())
                .state(request.state())
                .city(request.city())
                .address(request.address())
                .phone(request.phone())
                .email(request.email())
                .build();

        Chemist saved = chemistRepository.save(chemist);
        return chemistMapper.toDto(
                chemistRepository.findByIdWithDetails(saved.getId()).orElseThrow()
        );
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    @Transactional
    public ChemistDto updateChemist(UUID id, UpdateChemistRequest request){
        Chemist chemist = chemistRepository.findByIdWithDetails(id)
                .orElseThrow(()->new ResourceNotFoundException("Chemist","id",id));

        if (!chemist.isActive() &&
                (request.isActive() == null || !request.isActive())) {
            throw new IllegalArgumentException(
                    "Cannot update a deactivated chemist: "
                            + chemist.getFirmName()
                            + ". Set isActive to true to reactivate first.");
        }

        User rep = userRepository.findById(request.assignedRepId())
                .orElseThrow(()->new ResourceNotFoundException("User","id",request.assignedRepId()));

        if (!rep.isActive()) {
            throw new IllegalArgumentException(
                    "Cannot assign chemist to deactivated rep: " + rep.getFullName());
        }

        if(!request.drugLicenseNumber().equals(chemist.getDrugLicenseNumber())){
            if(chemistRepository.existsByDrugLicenseNumber(request.drugLicenseNumber())){
                throw new IllegalArgumentException(
                        "Chemist with Drug License Number already exists: " + request.drugLicenseNumber());
            }
        }

        if(request.gstin() != null && !request.gstin().isBlank()){
            if(!request.gstin().equals(chemist.getGstin())){
                if(chemistRepository.existsByGstin(request.gstin())){
                    throw new IllegalArgumentException("Chemist with GSTIN exists: "+request.gstin());
                }
            }
        }

        chemist.setAssignedRep(rep);
        chemist.setFirmName(request.firmName());
        chemist.setOwnerName(request.ownerName());
        chemist.setDrugLicenseNumber(request.drugLicenseNumber());
        chemist.setGstin(request.gstin());
        chemist.setState(request.state());
        chemist.setCity(request.city());
        chemist.setAddress(request.address());
        chemist.setPhone(request.phone());
        chemist.setEmail(request.email());

        if(request.isActive() != null){
            chemist.setActive(request.isActive());
        }

        chemistRepository.save(chemist);
        return chemistMapper.toDto(
                chemistRepository.findByIdWithDetails(id).orElseThrow()
        );
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    @Transactional
    public void deactivateChemist(UUID id) {
        Chemist chemist = chemistRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chemist", "id", id));
        chemist.setActive(false);
        chemistRepository.save(chemist);
    }

}
