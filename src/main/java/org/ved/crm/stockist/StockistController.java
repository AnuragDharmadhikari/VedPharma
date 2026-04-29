package org.ved.crm.stockist;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ved.crm.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stockists")
@RequiredArgsConstructor
public class StockistController {

    private final StockistService stockistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StockistDto>>> getAllActiveStockists() {
        return ResponseEntity.ok(
                ApiResponse.success("Stockists retrieved successfully",
                        stockistService.getAllActiveStockists()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StockistDto>> getStockistById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("Stockist retrieved successfully",
                        stockistService.getStockistById(id)));
    }

    @GetMapping("/rep/{repId}")
    public ResponseEntity<ApiResponse<List<StockistDto>>> getStockistsByRep(
            @PathVariable UUID repId) {
        return ResponseEntity.ok(
                ApiResponse.success("Stockists retrieved successfully",
                        stockistService.getStockistsByRep(repId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockistDto>> createStockist(
            @Valid @RequestBody CreateStockistRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Stockist created successfully",
                        stockistService.createStockist(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StockistDto>> updateStockist(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStockistRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Stockist updated successfully",
                        stockistService.updateStockist(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateStockist(
            @PathVariable UUID id) {
        stockistService.deactivateStockist(id);
        return ResponseEntity.ok(
                ApiResponse.success("Stockist deactivated successfully"));
    }
}