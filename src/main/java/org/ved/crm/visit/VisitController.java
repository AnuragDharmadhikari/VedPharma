package org.ved.crm.visit;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ved.crm.common.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
public class VisitController {
    private final VisitService visitService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VisitDto>>> getAllVisits(){
        return ResponseEntity.ok(ApiResponse.success("Visits retrieved successfully",visitService.getAllVisits()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VisitDto>> getVisitById(@PathVariable UUID id){
        return ResponseEntity.ok(ApiResponse.success("Visit retrieved successfully",visitService.getVisitById(id)));

    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<VisitDto>>> getVisitsByDoctor(@PathVariable UUID doctorId){
        return ResponseEntity.ok(ApiResponse.success("Visits retrieved successfully",visitService.getVisitsByDoctor(doctorId)));
    }

    @GetMapping("/rep/{repId}")
    public ResponseEntity<ApiResponse<List<VisitDto>>> getVisitsByRep(
            @PathVariable UUID repId) {
        return ResponseEntity.ok(
                ApiResponse.success("Visits retrieved successfully",
                        visitService.getVisitsByRep(repId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VisitDto>> createVisit(@Valid @RequestBody CreateVisitRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Visit created successfully",visitService.createVisit(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VisitDto>> updateVisit(@PathVariable UUID id,@Valid @RequestBody UpdateVisitRequest request){
        return ResponseEntity.ok(ApiResponse.success("Visit updated successfully",visitService.updateVisit(id,request)));
    }
}
