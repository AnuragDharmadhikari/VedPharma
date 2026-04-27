package org.ved.crm.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ved.crm.common.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductDto> getAllActiveProducts(){
        return productRepository.findByIsActiveTrue()
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    public ProductDto getProductById(UUID id){
        return productRepository.findById(id)
                .map(productMapper::toDto)
                .orElseThrow(()->new ResourceNotFoundException("Product","id",id));
    }

    public List<ProductDto> getProductsByCategory(String category) {
        return productRepository.findByCategoryAndIsActiveTrue(category)
                .stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request){
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
        return productMapper.toDto(saved);

    }

    @Transactional
    public ProductDto updateProduct(UUID id,UpdateProductRequest request){
        Product product = productRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Product","id",id));

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

    @Transactional
    public void deactivateProduct(UUID id){
        Product product = productRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Product","id",id));
        product.setActive(false);
        productRepository.save(product);
    }

}
