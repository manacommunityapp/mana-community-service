package com.manacommunity.api.fitprofile;

import com.manacommunity.api.model.JewelleryProduct;
import com.manacommunity.api.model.JewelleryProductAsset;
import com.manacommunity.api.repository.JewelleryProductAssetRepository;
import com.manacommunity.api.repository.JewelleryProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JewelleryProductCatalogPortAdapter implements ProductCatalogPort {

    private final JewelleryProductRepository productRepo;
    private final JewelleryProductAssetRepository assetRepo;

    @Override
    public List<Candidate> availableByCategory(String category, int max) {
        List<JewelleryProduct> products = productRepo.findByCategoryAndStatusOrderByCreatedAtDesc(
                category.toUpperCase(), "ACTIVE");

        return products.stream()
                .limit(max)
                .map(p -> new Candidate(
                        p.getId(),
                        p.getName(),
                        p.getTotalPrice() != null ? p.getTotalPrice() : BigDecimal.ZERO
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductRef> findAvailable(Long productId) {
        return productRepo.findByIdAndStatus(productId, "ACTIVE")
                .map(p -> {
                    String img = assetRepo.findByProductAndAssetTypeAndApprovalStatus(p, "IMAGE_MAIN", "APPROVED")
                            .stream()
                            .findFirst()
                            .map(JewelleryProductAsset::getAssetUrl)
                            .orElse(null);
                    return new ProductRef(
                            p.getId(),
                            p.getCategory(),
                            img
                    );
                });
    }
}
