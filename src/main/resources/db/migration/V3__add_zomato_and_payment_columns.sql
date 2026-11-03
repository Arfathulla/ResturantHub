ALTER TABLE orders
    ADD COLUMN zomato_order_id VARCHAR(100) NULL,
    ADD COLUMN zomato_sync_status VARCHAR(50) NULL,
    ADD COLUMN payment_method VARCHAR(50) NULL;

ALTER TABLE payments
    ADD COLUMN payment_method VARCHAR(50) NULL,
    ADD COLUMN payment_details VARCHAR(255) NULL;
