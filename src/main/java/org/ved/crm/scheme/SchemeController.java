package org.ved.crm.scheme;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ved.crm.common.ApiResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schemes")
@RequiredArgsConstructor
public class SchemeController {

    private final SchemeService schemeService;

    // Get all active schemes for a specific chemist.
    // Rep or owner checks what deals are active for a chemist
    // before placing an order.

    @GetMapping("/chemist/{chemistId}")
    public ResponseEntity<ApiResponse<List<SchemeDto>>> getSchemeByChemist(@PathVariable UUID chemistId){
        return ResponseEntity.ok(ApiResponse.success("Schemes retrieved successfully",schemeService.getSchemesByChemist(chemistId)));
    }

    // Get all active schemes for a specific stockist.
    @GetMapping("/stockist/{stockistId}")
    public ResponseEntity<ApiResponse<List<SchemeDto>>> getSchemesByStockist(
            @PathVariable UUID stockistId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Schemes retrieved successfully",
                        schemeService.getSchemesByStockist(stockistId)
                )
        );
    }

    // Get scheme by ID — used for detail view and debugging.
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SchemeDto>> getSchemeById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scheme retrieved successfully",
                        schemeService.getSchemeById(id)
                )
        );
    }

    // Create a new scheme — owner negotiates deal with buyer,
    // enters it into the system. Auto-applies on next qualifying order.
    @PostMapping
    public ResponseEntity<ApiResponse<SchemeDto>> createScheme(
            @Valid @RequestBody CreateSchemeRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Scheme created successfully",
                        schemeService.createScheme(request)
                ));
    }

    // Update scheme — renegotiated terms, extended validity, or deactivation.
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SchemeDto>> updateScheme(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSchemeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Scheme updated successfully",
                        schemeService.updateScheme(id, request)
                )
        );
    }

}
