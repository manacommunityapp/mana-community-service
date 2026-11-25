-- Align PostgreSQL enum columns on delivery_slot and payment_order with JPA @Enumerated(EnumType.STRING)
-- so Hibernate 6 parameterized queries never fail with "operator does not exist: <enum> = character varying".

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'manacommunity' AND table_name = 'delivery_slot'
          AND column_name = 'status' AND data_type = 'USER-DEFINED'
    ) THEN
        ALTER TABLE manacommunity.delivery_slot ALTER COLUMN status DROP DEFAULT;
        ALTER TABLE manacommunity.delivery_slot ALTER COLUMN status TYPE VARCHAR(30) USING status::text;
        ALTER TABLE manacommunity.delivery_slot ALTER COLUMN status SET DEFAULT 'OPEN';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'manacommunity' AND table_name = 'payment_order'
          AND column_name = 'gateway' AND data_type = 'USER-DEFINED'
    ) THEN
        ALTER TABLE manacommunity.payment_order ALTER COLUMN gateway TYPE VARCHAR(20) USING gateway::text;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'manacommunity' AND table_name = 'payment_order'
          AND column_name = 'reference_type' AND data_type = 'USER-DEFINED'
    ) THEN
        ALTER TABLE manacommunity.payment_order ALTER COLUMN reference_type TYPE VARCHAR(30) USING reference_type::text;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'manacommunity' AND table_name = 'payment_order'
          AND column_name = 'status' AND data_type = 'USER-DEFINED'
    ) THEN
        ALTER TABLE manacommunity.payment_order ALTER COLUMN status DROP DEFAULT;
        ALTER TABLE manacommunity.payment_order ALTER COLUMN status TYPE VARCHAR(25) USING status::text;
        ALTER TABLE manacommunity.payment_order ALTER COLUMN status SET DEFAULT 'INITIATED';
    END IF;
END $$;
