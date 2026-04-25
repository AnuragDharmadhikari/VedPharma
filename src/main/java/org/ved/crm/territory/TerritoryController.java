package org.ved.crm.territory;

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
@RequestMapping("/api/v1/territories")
public class TerritoryController {

    private final TerritoryService territoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TerritoryDto>>> getAllTerritories(){
        List<TerritoryDto> territories = territoryService.getAllTerritories();
        return ResponseEntity.ok(ApiResponse.success("Territories retrieved successfully",territories));
    }

    @GetMapping("{id}")
    public ResponseEntity<ApiResponse<TerritoryDto>> getTerritoryById(@PathVariable UUID id){
        TerritoryDto territory = territoryService.getTerritoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Territory retrieved successfully",territory));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TerritoryDto>> createTerritory(@Valid @RequestBody CreateTerritoryRequest request){
        TerritoryDto territory = territoryService.createTerritory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Territory created successfully",territory));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TerritoryDto>> updateTerritory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTerritoryRequest request) {
        TerritoryDto territory = territoryService.updateTerritory(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Territory updated successfully", territory));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateTerritory(@PathVariable UUID id){
        territoryService.deactivateTerritory(id);
        return ResponseEntity.ok(ApiResponse.success("Territory deactivated successfully"));
    }
}
