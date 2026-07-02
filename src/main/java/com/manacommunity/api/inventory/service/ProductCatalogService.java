package com.manacommunity.api.inventory.service;

import com.manacommunity.api.inventory.dto.ProductDto;
import com.manacommunity.api.inventory.entity.Product;
import com.manacommunity.api.inventory.mapper.InventoryMapper;
import com.manacommunity.api.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCatalogService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(InventoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        return InventoryMapper.toDto(p);
    }

    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        Product p = Product.builder()
                .name(dto.getName())
                .sku(dto.getSku())
                .barcode(dto.getBarcode())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .build();
        p = productRepository.save(p);
        return InventoryMapper.toDto(p);
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        p.setName(dto.getName());
        p.setSku(dto.getSku());
        p.setBarcode(dto.getBarcode());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p = productRepository.save(p);
        return InventoryMapper.toDto(p);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
