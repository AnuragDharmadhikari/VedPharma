package org.ved.crm.product;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.audit.Audited;
import org.ved.crm.common.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<ProductDto> getAllActiveProducts(){
        return productRepository.findByIsActiveTrue()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public ProductDto getProductById(UUID id){
        return productRepository.findById(id)
                .map(productMapper::toDto)
                .orElseThrow(()->new ResourceNotFoundException("Product","id",id));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'REP')")
    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndIsActiveTrue(category)
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ProductDto createProduct(CreateProductRequest request){

        if (request.dealerPrice().compareTo(request.mrp()) > 0) {
            throw new IllegalArgumentException(
                    "Dealer price (" + request.dealerPrice()
                            + ") cannot exceed MRP (" + request.mrp() + ")");
        }

        Product product = Product.builder()
                .name(request.name())
                .molecule(request.molecule())
                .category(request.category())
                .hsnCode(request.hsnCode())
                .gstRate(request.gstRate())
                .mrp(request.mrp())
                .dealerPrice(request.dealerPrice())
                .build();

        Product saved = productRepository.save(product);
        return productMapper.toDto(
                productRepository.findById(saved.getId()).orElseThrow());

    }

    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ProductDto updateProduct(UUID id,UpdateProductRequest request){
        Product product = productRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Product","id",id));

        if (!product.isActive() && (request.isActive() == null || !request.isActive())) {
            throw new IllegalArgumentException(
                    "Cannot update a discontinued product: " + product.getName()
                            + ". Set isActive to true to reactivate first.");
        }

        if (request.dealerPrice().compareTo(request.mrp()) > 0) {
            throw new IllegalArgumentException(
                    "Dealer price (" + request.dealerPrice()
                            + ") cannot exceed MRP (" + request.mrp() + ")");
        }

        product.setName(request.name());
        product.setMolecule(request.molecule());
        product.setCategory(request.category());
        product.setHsnCode(request.hsnCode());
        product.setGstRate(request.gstRate());
        product.setMrp(request.mrp());
        product.setDealerPrice(request.dealerPrice());

        if(request.isActive() != null){
            product.setActive(request.isActive());
        }

        productRepository.save(product);
        return productMapper.toDto(
                productRepository.findById(id).orElseThrow());
    }

    @Audited(action = "PRODUCT_DEACTIVATED", entityType = "Product")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public void deactivateProduct(UUID id){
        Product product = productRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Product","id",id));
        product.setActive(false);
        productRepository.save(product);
    }

}
