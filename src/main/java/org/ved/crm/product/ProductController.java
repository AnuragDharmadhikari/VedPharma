package org.ved.crm.product;

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
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllActiveProducts(){
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully",productService.getAllActiveProducts()));
    }

    @GetMapping("{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductById(@PathVariable UUID id){
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully",productService.getProductById(id)));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProductByCategory(@PathVariable String category){
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully",productService.getProductsByCategory(category)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(@Valid @RequestBody CreateProductRequest request){
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully",productService.createProduct(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request){
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully",productService.updateProduct(id,request)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateProduct(@PathVariable UUID id){
        return ResponseEntity.ok(ApiResponse.success("Product Deactivated Successfully"));
    }

}
