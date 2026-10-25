ALTER TABLE manacommunity.food_grocery_products
    ADD COLUMN IF NOT EXISTS vendor_variant_id BIGINT
        REFERENCES manacommunity.vendor_product_variant(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_food_grocery_products_vendor_variant
    ON manacommunity.food_grocery_products(vendor_variant_id)
    WHERE vendor_variant_id IS NOT NULL;
