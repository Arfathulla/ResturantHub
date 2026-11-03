-- =====================================================================
-- RestaurantHub Flyway Migration V2: Seed Initial Master Data
-- All default user accounts have password: "password"
-- BCrypt: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
-- =====================================================================

-- 1. Roles
INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_ADMIN', 'System Administrator with global privileges'),
(2, 'ROLE_MANAGER', 'Restaurant Branch Manager'),
(3, 'ROLE_STAFF', 'Floor & Front-Desk Staff'),
(4, 'ROLE_KITCHEN', 'Kitchen & Chef Staff'),
(5, 'ROLE_CUSTOMER', 'Standard Registered Customer');

-- 2. Users (Password is 'password')
INSERT INTO users (id, email, password_hash, first_name, last_name, phone_number, status, email_verified) VALUES
(1, 'admin@restauranthub.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'System', 'Admin', '+12025550101', 'ACTIVE', TRUE),
(2, 'manager@restauranthub.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Marcus', 'Vance', '+12025550102', 'ACTIVE', TRUE),
(3, 'staff@restauranthub.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Sarah', 'Jenkins', '+12025550103', 'ACTIVE', TRUE),
(4, 'kitchen@restauranthub.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Gordon', 'Ramsay', '+12025550104', 'ACTIVE', TRUE),
(5, 'customer@restauranthub.com', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Alice', 'Walker', '+12025550105', 'ACTIVE', TRUE),
(6, 'arfath119@gmail.com', '$2a$10$TMHTMokIgz7WTJStpF/s6uezygmpS/AVbMv7HPUI3uZDhvFV8uBxS', 'Arfath', 'AS', '+917022184349', 'ACTIVE', TRUE);

-- User Role Assignments
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), -- Admin
(2, 2), -- Manager
(3, 3), -- Staff
(4, 4), -- Kitchen
(5, 5), -- Customer
(6, 1), -- Admin
(6, 5); -- Customer

-- 3. Restaurant Branches
INSERT INTO restaurant_branches (id, name, code, description, address, city, state, postal_code, phone_number, email, time_zone, image_url, is_active) VALUES
(1, 'Downtown Gourmet Flagship', 'DT-GOURMET', 'Our premier culinary destination featuring wood-fired grill, craft cocktails, and luxurious private dining.', '100 Main Street', 'New York', 'NY', '10001', '+12125550190', 'downtown@restauranthub.com', 'America/New_York', 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800', TRUE),
(2, 'Midtown Bistro & Lounge', 'MT-BISTRO', 'Chic urban dining with skyline views, bespoke event halls, and artisanal Italian delicacies.', '500 5th Avenue', 'New York', 'NY', '10018', '+12125550199', 'midtown@restauranthub.com', 'America/New_York', 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800', TRUE);

-- Staff Branch Assignments
INSERT INTO staff_branch_assignments (user_id, branch_id, designation) VALUES
(2, 1, 'General Manager'),
(3, 1, 'Head Waiter'),
(4, 1, 'Executive Chef');

-- Operating Hours (Branch 1)
INSERT INTO operating_hours (branch_id, day_of_week, open_time, close_time, is_closed) VALUES
(1, 'MONDAY', '09:00:00', '23:00:00', FALSE),
(1, 'TUESDAY', '09:00:00', '23:00:00', FALSE),
(1, 'WEDNESDAY', '09:00:00', '23:00:00', FALSE),
(1, 'THURSDAY', '09:00:00', '23:00:00', FALSE),
(1, 'FRIDAY', '09:00:00', '23:59:00', FALSE),
(1, 'SATURDAY', '09:00:00', '23:59:00', FALSE),
(1, 'SUNDAY', '09:00:00', '22:00:00', FALSE);

-- Operating Hours (Branch 2)
INSERT INTO operating_hours (branch_id, day_of_week, open_time, close_time, is_closed) VALUES
(2, 'MONDAY', '10:00:00', '23:00:00', FALSE),
(2, 'TUESDAY', '10:00:00', '23:00:00', FALSE),
(2, 'WEDNESDAY', '10:00:00', '23:00:00', FALSE),
(2, 'THURSDAY', '10:00:00', '23:00:00', FALSE),
(2, 'FRIDAY', '10:00:00', '23:59:00', FALSE),
(2, 'SATURDAY', '10:00:00', '23:59:00', FALSE),
(2, 'SUNDAY', '10:00:00', '22:00:00', FALSE);

-- 4. Dining Tables (Branch 1)
INSERT INTO dining_tables (id, branch_id, table_number, seating_capacity, table_type, section, is_accessible, deposit_amount, is_active) VALUES
(1, 1, 'T-01', 2, 'STANDARD', 'MAIN_DINING', TRUE, 10.00, TRUE),
(2, 1, 'T-02', 4, 'BOOTH', 'WINDOW', TRUE, 15.00, TRUE),
(3, 1, 'T-03', 6, 'STANDARD', 'MAIN_DINING', TRUE, 20.00, TRUE),
(4, 1, 'T-04', 8, 'HIGH_TOP', 'PATIO', TRUE, 25.00, TRUE),
(5, 1, 'T-05', 2, 'STANDARD', 'PATIO', TRUE, 10.00, TRUE);

-- Dining Tables (Branch 2)
INSERT INTO dining_tables (id, branch_id, table_number, seating_capacity, table_type, section, is_accessible, deposit_amount, is_active) VALUES
(6, 2, 'M-01', 2, 'STANDARD', 'MAIN_DINING', TRUE, 10.00, TRUE),
(7, 2, 'M-02', 4, 'BOOTH', 'MAIN_DINING', TRUE, 15.00, TRUE),
(8, 2, 'M-03', 6, 'STANDARD', 'WINDOW', TRUE, 20.00, TRUE);

-- 5. Dining Rooms
INSERT INTO dining_rooms (id, branch_id, room_name, capacity, description, amenities, base_price, hourly_price, deposit_amount, min_duration_hours, max_duration_hours, image_url, is_active) VALUES
(1, 1, 'The Royal Oak Suite', 14, 'Intimate luxury private dining with dedicated sommelier and surround sound.', '4K Display, Sonos Sound, Dedicated Butler, Wine Chiller', 100.00, 60.00, 50.00, 2, 6, 'https://images.unsplash.com/photo-1544025162-d76694265947?w=800', TRUE),
(2, 2, 'The Executive Cellar', 10, 'Rustic wine cellar setting for corporate meetings and executive dinners.', 'Conference Tech, Acoustic Paneling, Private Restroom', 80.00, 50.00, 40.00, 1, 5, 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800', TRUE);

-- 6. Party Halls & Packages
INSERT INTO party_halls (id, branch_id, hall_name, capacity, description, base_rental_price, hourly_rate, deposit_amount, min_duration_hours, max_duration_hours, image_url, is_active) VALUES
(1, 1, 'Grand Crystal Ballroom', 150, 'Spacious ballroom suited for lavish wedding receptions, anniversary banquets, and corporate galas.', 500.00, 150.00, 250.00, 3, 10, 'https://images.unsplash.com/photo-1519167758481-83f550bb49b3?w=800', TRUE),
(2, 2, 'Skyline Terrace Hall', 80, 'Indoor-outdoor panoramic rooftop hall with city views and cocktail stage.', 350.00, 100.00, 150.00, 2, 8, 'https://images.unsplash.com/photo-1464366400600-7168b8af9bc3?w=800', TRUE);

INSERT INTO hall_packages (id, hall_id, package_name, price_per_guest, description, is_active) VALUES
(1, 1, 'Silver Gala Buffet', 45.00, '3-course buffet, welcome drinks, table linens, and background sound system.', TRUE),
(2, 1, 'Platinum Imperial Feast', 75.00, '5-course plated dinner, open bar for 3 hours, live stage, and customized floral decor.', TRUE),
(3, 2, 'Corporate Reception & Canapes', 35.00, 'Cocktail snacks, artisanal sliders, champagne toast, and presentation projector.', TRUE);

-- 7. Parking Slots
INSERT INTO parking_slots (id, branch_id, slot_number, vehicle_type, hourly_rate, is_covered, is_accessible, is_active) VALUES
(1, 1, 'P-01', 'CAR', 5.00, TRUE, FALSE, TRUE),
(2, 1, 'P-02', 'CAR', 5.00, TRUE, FALSE, TRUE),
(3, 1, 'P-03', 'EV', 7.50, TRUE, TRUE, TRUE),
(4, 1, 'P-04', 'MOTORCYCLE', 3.00, TRUE, FALSE, TRUE),
(5, 2, 'PM-01', 'CAR', 6.00, TRUE, FALSE, TRUE),
(6, 2, 'PM-02', 'CAR', 6.00, FALSE, FALSE, TRUE);

-- 8. Menu Categories
INSERT INTO menu_categories (id, name, display_order, is_active) VALUES
(1, 'Appetizers & Starters', 1, TRUE),
(2, 'Artisanal Mains', 2, TRUE),
(3, 'Wood-Fired Pizza & Pasta', 3, TRUE),
(4, 'Decadent Desserts', 4, TRUE),
(5, 'Craft Beverages & Cocktails', 5, TRUE);

-- 9. Menu Items (Branch 1)
INSERT INTO menu_items (id, branch_id, category_id, name, description, base_price, image_url, is_vegetarian, is_vegan, is_gluten_free, allergen_info, prep_time_minutes, tax_rate_percent, is_available) VALUES
(1, 1, 1, 'Crispy Calamari Rings', 'Flash-fried tender calamari served with lemon caper aioli and marinara dip.', 16.00, 'https://images.unsplash.com/photo-1599488615731-7e5c2823ff28?w=800', FALSE, FALSE, FALSE, 'Seafood, Gluten', 12, 5.00, TRUE),
(2, 1, 1, 'Bruschetta al Pomodoro', 'Toasted sourdough rubbed with garlic, heirloom tomatoes, fresh basil, and aged balsamic glaze.', 12.00, 'https://images.unsplash.com/photo-1572695157366-5e585ab2b69f?w=800', TRUE, TRUE, FALSE, 'Gluten', 10, 5.00, TRUE),
(3, 1, 2, 'Truffle Wild Mushroom Risotto', 'Slow-cooked Carnaroli rice, wild forest mushrooms, black truffle paste, and 24-month aged Parmigiano.', 24.00, 'https://images.unsplash.com/photo-1633964913295-ceb43826e7c9?w=800', TRUE, FALSE, TRUE, 'Dairy', 20, 5.00, TRUE),
(4, 1, 2, 'Prime Black Angus Ribeye Steak', '12oz prime cut, garlic rosemary butter, truffle mashed potatoes, and charred broccolini.', 38.00, 'https://images.unsplash.com/photo-1544025162-d76694265947?w=800', FALSE, FALSE, TRUE, 'Dairy', 25, 5.00, TRUE),
(5, 1, 3, 'Wood-Fired Margherita D.O.P.', 'San Marzano tomatoes, fresh Fior di Latte mozzarella, basil, and extra virgin olive oil.', 18.00, 'https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=800', TRUE, FALSE, FALSE, 'Gluten, Dairy', 15, 5.00, TRUE),
(6, 1, 4, 'Classic Artisanal Tiramisu', 'Espresso-soaked Savoiardi ladyfingers, velvety mascarpone cream, and dark cocoa powder.', 11.00, 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=800', TRUE, FALSE, FALSE, 'Dairy, Eggs, Gluten', 8, 5.00, TRUE),
(7, 1, 5, 'Signature House Sangria', 'Full-bodied Spanish red wine infused with fresh citrus, brandy, and wild berries.', 14.00, 'https://images.unsplash.com/photo-1551024709-8f23befc6f87?w=800', TRUE, TRUE, TRUE, 'Alcohol', 5, 5.00, TRUE);

-- Variants & Addons
INSERT INTO menu_item_variants (id, menu_item_id, variant_name, price_delta) VALUES
(1, 5, 'Regular (10 inch)', 0.00),
(2, 5, 'Large (14 inch)', 6.00);

INSERT INTO menu_item_addons (id, menu_item_id, addon_name, price) VALUES
(1, 4, 'Extra Truffle Butter', 3.50),
(2, 4, 'Grilled Tiger Prawns (Surf & Turf)', 9.00),
(3, 5, 'Buffalo Mozzarella Upgrade', 4.00),
(4, 5, 'Prosciutto di Parma Topping', 5.00);

-- Inventory Tracking for Menu Items
INSERT INTO inventory_items (id, menu_item_id, quantity_in_stock, low_stock_threshold) VALUES
(1, 1, 45, 10),
(2, 2, 60, 10),
(3, 3, 40, 8),
(4, 4, 30, 5),
(5, 5, 55, 10),
(6, 6, 25, 5),
(7, 7, 70, 10);

-- 10. Coupons
INSERT INTO coupons (id, code, discount_type, discount_value, min_order_amount, max_discount_amount, usage_limit, used_count, valid_from, valid_until, is_active) VALUES
(1, 'WELCOME20', 'PERCENTAGE', 20.00, 30.00, 25.00, 500, 0, '2025-01-01 00:00:00', '2027-12-31 23:59:59', TRUE),
(2, 'FLAT10', 'FIXED_AMOUNT', 10.00, 40.00, 10.00, 1000, 0, '2025-01-01 00:00:00', '2027-12-31 23:59:59', TRUE);
