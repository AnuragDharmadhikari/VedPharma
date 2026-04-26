package org.ved.crm.product;

import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductDto toDto(Product product){
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getMolecule(),
                product.getCategory(),
                product.getHsnCode(),
                product.getGstRate(),
                product.getGstRate().getRate(),
                product.getMrp(),
                product.getDealerPrice(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
