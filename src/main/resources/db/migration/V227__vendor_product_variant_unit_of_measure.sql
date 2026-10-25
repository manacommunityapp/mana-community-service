ALTER TABLE manacommunity.vendor_product_variant
    ADD COLUMN IF NOT EXISTS unit_of_measure VARCHAR(30);
