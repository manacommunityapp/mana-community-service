-- V259: Backfill existing Group Buy Orders into Commerce Orders

DO $$
DECLARE
    gbo RECORD;
    v_commerce_order_id BIGINT;
    v_commerce_status VARCHAR(32);
    v_channel_ref VARCHAR(64);
    v_vendor_id VARCHAR(64);
    v_vendor_name VARCHAR(150);
    v_img_url TEXT;
BEGIN
    FOR gbo IN
        SELECT o.*, d.vendor_id AS deal_vendor_id, d.vendor_name AS deal_vendor_name, d.image_url AS deal_image_url
        FROM manacommunity.group_buy_order o
        LEFT JOIN manacommunity.group_buy_deal d ON o.deal_id = d.id
        WHERE o.commerce_order_id IS NULL
    LOOP
        -- Map status
        CASE gbo.status
            WHEN 'CONFIRMED' THEN v_commerce_status := 'CONFIRMED';
            WHEN 'PREPARING' THEN v_commerce_status := 'PROCESSING';
            WHEN 'OUT_FOR_DELIVERY' THEN v_commerce_status := 'OUT_FOR_DELIVERY';
            WHEN 'READY_FOR_PICKUP' THEN v_commerce_status := 'READY_FOR_PICKUP';
            WHEN 'DELIVERED' THEN v_commerce_status := 'COMPLETED';
            WHEN 'PICKED_UP' THEN v_commerce_status := 'COMPLETED';
            WHEN 'CANCELLED' THEN v_commerce_status := 'CANCELLED';
            WHEN 'REFUNDED' THEN v_commerce_status := 'REFUNDED';
            ELSE v_commerce_status := 'CONFIRMED';
        END CASE;

        v_channel_ref := CAST(gbo.deal_id AS VARCHAR(64));
        v_vendor_id := COALESCE(gbo.deal_vendor_id, 'VND-LOCAL');
        v_vendor_name := COALESCE(gbo.deal_vendor_name, 'Community Vendor');
        v_img_url := gbo.deal_image_url;

        -- Check if commerce_order already exists with this order_number
        SELECT id INTO v_commerce_order_id FROM manacommunity.commerce_order WHERE order_number = gbo.order_number;

        IF v_commerce_order_id IS NULL THEN
            INSERT INTO manacommunity.commerce_order (
                order_number,
                channel,
                buyer_id,
                seller_id,
                vendor_id,
                vendor_name,
                community_id,
                channel_reference_id,
                status,
                subtotal_amount,
                discount_amount,
                delivery_fee,
                tax_amount,
                total_amount,
                savings_amount,
                payment_method,
                payment_status,
                fulfillment_type,
                delivery_address,
                pickup_point,
                handover_otp,
                qr_token,
                is_escrow_locked,
                created_at,
                updated_at
            ) VALUES (
                gbo.order_number,
                'GROUP_BUY',
                gbo.user_id,
                NULL,
                v_vendor_id,
                v_vendor_name,
                gbo.community_id,
                v_channel_ref,
                v_commerce_status,
                gbo.total_amount,
                0,
                0,
                0,
                gbo.total_amount,
                COALESCE(gbo.savings_amount, 0),
                COALESCE(gbo.payment_method, 'UPI'),
                COALESCE(gbo.payment_status, 'PAID'),
                COALESCE(gbo.pickup_point, 'CLUBHOUSE_PICKUP'),
                gbo.delivery_address,
                gbo.pickup_point,
                SUBSTRING(COALESCE(gbo.delivery_otp, '123456') FROM 1 FOR 8),
                COALESCE(gbo.qr_token, 'QR-GB-' || gbo.order_number),
                TRUE,
                gbo.created_at,
                gbo.updated_at
            ) RETURNING id INTO v_commerce_order_id;

            INSERT INTO manacommunity.commerce_order_item (
                order_id,
                product_id,
                title,
                unit_price,
                quantity,
                total_price,
                image_url
            ) VALUES (
                v_commerce_order_id,
                v_channel_ref,
                gbo.deal_title,
                gbo.unit_price,
                gbo.quantity,
                gbo.total_amount,
                v_img_url
            );
        END IF;

        UPDATE manacommunity.group_buy_order
        SET commerce_order_id = v_commerce_order_id,
            legacy_unverified = TRUE
        WHERE id = gbo.id;
    END LOOP;
END $$;
