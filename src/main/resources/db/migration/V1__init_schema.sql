-- =====================================================================
-- RestaurantHub Flyway Migration V1: Complete Relational Schema
-- =====================================================================

-- 1. Users and Authentication
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE user_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(30) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'USA',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pwd_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 2. Restaurant Branches & Facilities
CREATE TABLE restaurant_branches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(30) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    email VARCHAR(150) NOT NULL,
    time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE operating_hours (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    open_time TIME NOT NULL,
    close_time TIME NOT NULL,
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_op_hours_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE,
    CONSTRAINT uq_branch_day UNIQUE (branch_id, day_of_week)
);

CREATE TABLE restaurant_closures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    closure_date DATE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    CONSTRAINT fk_closures_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE
);

CREATE TABLE staff_branch_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    designation VARCHAR(100),
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_branch_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_branch_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE,
    CONSTRAINT uq_staff_branch UNIQUE (user_id, branch_id)
);

-- 3. Dining Tables & Reservations
CREATE TABLE dining_tables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    table_number VARCHAR(50) NOT NULL,
    seating_capacity INT NOT NULL,
    table_type VARCHAR(50) NOT NULL,
    section VARCHAR(50) NOT NULL,
    is_accessible BOOLEAN NOT NULL DEFAULT TRUE,
    deposit_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tables_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE,
    CONSTRAINT uq_branch_table UNIQUE (branch_id, table_number)
);

CREATE TABLE table_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    guest_count INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    special_requests TEXT,
    deposit_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    deposit_paid BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_table_res_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_table_res_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id),
    CONSTRAINT fk_table_res_table FOREIGN KEY (table_id) REFERENCES dining_tables(id)
);
CREATE INDEX idx_table_res_lookup ON table_reservations (table_id, reservation_date, status);

-- 4. Private Dining Rooms & Reservations
CREATE TABLE dining_rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    room_name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    description TEXT,
    amenities TEXT,
    base_price DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    hourly_price DECIMAL(10,2) NOT NULL DEFAULT 50.00,
    deposit_amount DECIMAL(10,2) NOT NULL DEFAULT 50.00,
    min_duration_hours INT NOT NULL DEFAULT 1,
    max_duration_hours INT NOT NULL DEFAULT 8,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rooms_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE
);

CREATE TABLE room_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    guest_count INT NOT NULL,
    duration_hours INT NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    deposit_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    special_requests TEXT,
    expires_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_res_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_room_res_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id),
    CONSTRAINT fk_room_res_room FOREIGN KEY (room_id) REFERENCES dining_rooms(id)
);
CREATE INDEX idx_room_res_lookup ON room_reservations (room_id, reservation_date, status);

-- 5. Party Halls & Reservations
CREATE TABLE party_halls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    hall_name VARCHAR(150) NOT NULL,
    capacity INT NOT NULL,
    description TEXT,
    base_rental_price DECIMAL(10,2) NOT NULL,
    hourly_rate DECIMAL(10,2) NOT NULL DEFAULT 100.00,
    deposit_amount DECIMAL(10,2) NOT NULL DEFAULT 200.00,
    min_duration_hours INT NOT NULL DEFAULT 2,
    max_duration_hours INT NOT NULL DEFAULT 12,
    image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_halls_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE
);

CREATE TABLE hall_packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hall_id BIGINT NOT NULL,
    package_name VARCHAR(150) NOT NULL,
    price_per_guest DECIMAL(10,2) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_packages_hall FOREIGN KEY (hall_id) REFERENCES party_halls(id) ON DELETE CASCADE
);

CREATE TABLE hall_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    hall_id BIGINT NOT NULL,
    package_id BIGINT NULL,
    event_type VARCHAR(100) NOT NULL,
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    guest_count INT NOT NULL,
    duration_hours INT NOT NULL,
    hall_rental_charge DECIMAL(10,2) NOT NULL,
    package_charge DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_price DECIMAL(10,2) NOT NULL,
    deposit_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    special_requests TEXT,
    expires_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_hall_res_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_hall_res_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id),
    CONSTRAINT fk_hall_res_hall FOREIGN KEY (hall_id) REFERENCES party_halls(id),
    CONSTRAINT fk_hall_res_pkg FOREIGN KEY (package_id) REFERENCES hall_packages(id)
);
CREATE INDEX idx_hall_res_lookup ON hall_reservations (hall_id, reservation_date, status);

-- 6. Parking Facilities & Reservations
CREATE TABLE parking_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    slot_number VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL,
    hourly_rate DECIMAL(10,2) NOT NULL DEFAULT 5.00,
    is_covered BOOLEAN NOT NULL DEFAULT TRUE,
    is_accessible BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_parking_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE,
    CONSTRAINT uq_branch_slot UNIQUE (branch_id, slot_number)
);

CREATE TABLE parking_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    parking_slot_id BIGINT NOT NULL,
    vehicle_registration_no VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL,
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_park_res_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_park_res_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id),
    CONSTRAINT fk_park_res_slot FOREIGN KEY (parking_slot_id) REFERENCES parking_slots(id)
);
CREATE INDEX idx_parking_res_lookup ON parking_reservations (parking_slot_id, reservation_date, status);

CREATE TABLE parking_checkin_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parking_reservation_id BIGINT NOT NULL,
    actual_entry_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actual_exit_time TIMESTAMP NULL,
    handled_by_user_id BIGINT NULL,
    CONSTRAINT fk_chk_res FOREIGN KEY (parking_reservation_id) REFERENCES parking_reservations(id) ON DELETE CASCADE
);

-- 7. Menu & Inventory
CREATE TABLE menu_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    base_price DECIMAL(10,2) NOT NULL,
    image_url VARCHAR(500),
    is_vegetarian BOOLEAN NOT NULL DEFAULT FALSE,
    is_vegan BOOLEAN NOT NULL DEFAULT FALSE,
    is_gluten_free BOOLEAN NOT NULL DEFAULT FALSE,
    allergen_info VARCHAR(255),
    prep_time_minutes INT NOT NULL DEFAULT 15,
    tax_rate_percent DECIMAL(5,2) NOT NULL DEFAULT 5.00,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_items_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id) ON DELETE CASCADE,
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES menu_categories(id)
);

CREATE TABLE menu_item_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id BIGINT NOT NULL,
    variant_name VARCHAR(100) NOT NULL,
    price_delta DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_variants_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE
);

CREATE TABLE menu_item_addons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id BIGINT NOT NULL,
    addon_name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_addons_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE
);

CREATE TABLE inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_item_id BIGINT NOT NULL UNIQUE,
    quantity_in_stock INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 10,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE
);

CREATE TABLE inventory_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_item_id BIGINT NOT NULL,
    quantity_delta INT NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    reference_note VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inv_mov_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id) ON DELETE CASCADE
);

-- 8. Carts
CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    branch_id BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_carts_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id)
);

CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    quantity INT NOT NULL DEFAULT 1,
    special_instructions VARCHAR(255),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id),
    CONSTRAINT fk_cart_items_var FOREIGN KEY (variant_id) REFERENCES menu_item_variants(id)
);

CREATE TABLE cart_item_addons (
    cart_item_id BIGINT NOT NULL,
    addon_id BIGINT NOT NULL,
    PRIMARY KEY (cart_item_id, addon_id),
    CONSTRAINT fk_cart_item_add_item FOREIGN KEY (cart_item_id) REFERENCES cart_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_add_add FOREIGN KEY (addon_id) REFERENCES menu_item_addons(id)
);

-- 9. Coupons & Discounts
CREATE TABLE coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    max_discount_amount DECIMAL(10,2) NULL,
    usage_limit INT NOT NULL DEFAULT 1000,
    used_count INT NOT NULL DEFAULT 0,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0
);

-- 10. Orders & Order Items
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    order_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    table_reservation_id BIGINT NULL,
    delivery_address VARCHAR(500) NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    tax_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    final_total DECIMAL(10,2) NOT NULL,
    applied_coupon_id BIGINT NULL,
    special_instructions TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id),
    CONSTRAINT fk_orders_table_res FOREIGN KEY (table_reservation_id) REFERENCES table_reservations(id),
    CONSTRAINT fk_orders_coupon FOREIGN KEY (applied_coupon_id) REFERENCES coupons(id)
);

CREATE TABLE coupon_redemptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    redeemed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    CONSTRAINT fk_redemptions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_redemptions_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    item_name_snapshot VARCHAR(150) NOT NULL,
    unit_price_snapshot DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    special_instructions VARCHAR(255),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id),
    CONSTRAINT fk_order_items_var FOREIGN KEY (variant_id) REFERENCES menu_item_variants(id)
);

CREATE TABLE order_item_addons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    addon_name_snapshot VARCHAR(100) NOT NULL,
    addon_price_snapshot DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_order_addon_item FOREIGN KEY (order_item_id) REFERENCES order_items(id) ON DELETE CASCADE
);

CREATE TABLE order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by_user_id BIGINT NULL,
    comment VARCHAR(255),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_hist_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- 11. Payments & Refunds
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_reference VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    table_reservation_id BIGINT NULL,
    room_reservation_id BIGINT NULL,
    hall_reservation_id BIGINT NULL,
    parking_reservation_id BIGINT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    provider VARCHAR(50) NOT NULL,
    gateway_transaction_id VARCHAR(150),
    status VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_pay_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_pay_table_res FOREIGN KEY (table_reservation_id) REFERENCES table_reservations(id),
    CONSTRAINT fk_pay_room_res FOREIGN KEY (room_reservation_id) REFERENCES room_reservations(id),
    CONSTRAINT fk_pay_hall_res FOREIGN KEY (hall_reservation_id) REFERENCES hall_reservations(id),
    CONSTRAINT fk_pay_park_res FOREIGN KEY (parking_reservation_id) REFERENCES parking_reservations(id)
);

CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    refund_reference VARCHAR(64) NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    gateway_refund_id VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

CREATE TABLE payment_webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(150) NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. Reviews, Notifications & Audit Logs
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    table_reservation_id BIGINT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rev_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_rev_branch FOREIGN KEY (branch_id) REFERENCES restaurant_branches(id)
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NULL,
    details TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
