package org.ved.crm.doctor;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ved.crm.common.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DoctorDto>>> getAllActiveDoctors(){
        List<DoctorDto> doctors = doctorService.getAllActiveDoctors();
        return ResponseEntity.ok(ApiResponse.success("Doctors retrieved successfully",doctors));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorDto>> getDoctorById(@PathVariable UUID id){
        DoctorDto doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor retrieved successfully",doctor));
    }

    @GetMapping("/territory/{territoryId}")
    public ResponseEntity<ApiResponse<List<DoctorDto>>> getDoctorsByTerritory(@PathVariable UUID territoryId){
        List<DoctorDto> doctors = doctorService.getDoctorsByTerritory(territoryId);
        return ResponseEntity.ok(ApiResponse.success("Doctors of specific territory retrieved successfully",doctors));
    }

    @GetMapping("/specialty/{specialty}")
    public ResponseEntity<ApiResponse<List<DoctorDto>>> getDoctorsBySpecialty(@PathVariable String specialty){
        List<DoctorDto> doctors = doctorService.getDoctorsBySpecialty(specialty);
        return ResponseEntity.ok(ApiResponse.success("Doctors of specific specialty retrieved successfully",doctors));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DoctorDto>> createDoctor(@Valid @RequestBody CreateDoctorRequest request){
        DoctorDto doctor = doctorService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Doctor created successfully",doctor));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DoctorDto>> updateDoctor(@PathVariable UUID id,@Valid @RequestBody UpdateDoctorRequest request){
        DoctorDto doctor = doctorService.updateDoctor(id,request);
        return ResponseEntity.ok(ApiResponse.success("Doctor updated successfully",doctor));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateDoctor(@PathVariable UUID id){
        doctorService.deactivateDoctor(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor deactivated successfully"));
    }



}
