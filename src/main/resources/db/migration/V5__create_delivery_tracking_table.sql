-- =====================================================================
-- Bhojana Bangalore Flyway Migration V5: Delivery Tracking Module
-- =====================================================================

CREATE TABLE IF NOT EXISTS delivery_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL,
    current_location VARCHAR(255) NULL,
    driver_name VARCHAR(100) NULL,
    driver_phone VARCHAR(30) NULL,
    vehicle_number VARCHAR(50) NULL,
    estimated_delivery_minutes INT NULL,
    status_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_tracking_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Backfill delivery_tracking records for any existing orders
INSERT INTO delivery_tracking (order_id, status, status_updated_at, created_at, updated_at)
SELECT id, status, updated_at, created_at, updated_at
FROM orders
WHERE id NOT IN (SELECT order_id FROM delivery_tracking);
