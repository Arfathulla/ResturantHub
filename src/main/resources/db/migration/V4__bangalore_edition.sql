-- =====================================================================
-- V4: Bangalore Edition — Replace branches with real Bangalore locations
-- All amounts in INR (Indian Rupees)
-- =====================================================================

-- Clear existing branch-dependent data first
DELETE FROM inventory_items WHERE menu_item_id IN (SELECT id FROM menu_items WHERE branch_id IN (1,2));
DELETE FROM menu_item_addons WHERE menu_item_id IN (SELECT id FROM menu_items WHERE branch_id IN (1,2));
DELETE FROM menu_item_variants WHERE menu_item_id IN (SELECT id FROM menu_items WHERE branch_id IN (1,2));
DELETE FROM menu_items WHERE branch_id IN (1,2);
DELETE FROM hall_packages WHERE hall_id IN (SELECT id FROM party_halls WHERE branch_id IN (1,2));
DELETE FROM party_halls WHERE branch_id IN (1,2);
DELETE FROM dining_rooms WHERE branch_id IN (1,2);
DELETE FROM dining_tables WHERE branch_id IN (1,2);
DELETE FROM parking_slots WHERE branch_id IN (1,2);
DELETE FROM operating_hours WHERE branch_id IN (1,2);
DELETE FROM staff_branch_assignments WHERE branch_id IN (1,2);
UPDATE restaurant_branches SET is_active = FALSE WHERE id IN (1,2);
DELETE FROM restaurant_branches WHERE id IN (1,2);

-- Insert 5 Bangalore Branches
INSERT INTO restaurant_branches (id, name, code, description, address, city, state, postal_code, phone_number, email, time_zone, image_url, is_active) VALUES
(10, 'Spice Garden — MG Road', 'BLR-MGRD', 'Premier rooftop dining experience on Mahatma Gandhi Road with panoramic Bangalore skyline views, authentic Karnataka cuisine and craft cocktails.', '45, Residency Road, MG Road', 'Bengaluru', 'Karnataka', '560025', '+918025012345', 'mgroad@spicegarden.in', 'Asia/Kolkata', 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800', TRUE),
(11, 'Biryani House — Koramangala', 'BLR-KRMG', 'Legendary dum biryani specialists in the heart of Koramangala, serving Hyderabadi and Lucknowi biryanis since 2008 with live tandoor station.', '80 Feet Road, 4th Block, Koramangala', 'Bengaluru', 'Karnataka', '560034', '+918026789012', 'koramangala@biryanihouse.in', 'Asia/Kolkata', 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800', TRUE),
(12, 'The Punjabi Dhaba — Indiranagar', 'BLR-INDR', 'Rustic North Indian dhaba vibes with premium ambience in Indiranagar. Famous for butter chicken, dal makhani and freshly baked naans from clay oven.', '100 Feet Road, 12th Main, Indiranagar', 'Bengaluru', 'Karnataka', '560038', '+918028901234', 'indiranagar@punjabihub.in', 'Asia/Kolkata', 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=800', TRUE),
(13, 'South Indian Mess — Whitefield', 'BLR-WHTF', 'Authentic South Indian thali and tiffin serving Bengalureans in Whitefield tech corridor. Best idli, dosa, vada, sambar and filter coffee in East Bangalore.', 'ITPL Main Road, Whitefield', 'Bengaluru', 'Karnataka', '560066', '+918022334455', 'whitefield@simes.in', 'Asia/Kolkata', 'https://images.unsplash.com/photo-1630383249896-424e482df921?w=800', TRUE),
(14, 'The Social Table — HSR Layout', 'BLR-HSRL', 'Trendy European-fusion gastropub in HSR Layout. Craft beers on tap, wood-fired pizzas, gourmet burgers and live weekend music for the Bangalore young professional crowd.', 'Sector 6, 27th Main Road, HSR Layout', 'Bengaluru', 'Karnataka', '560102', '+918024567890', 'hsr@socialtable.in', 'Asia/Kolkata', 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=800', TRUE);

-- Operating Hours for all 5 branches
INSERT INTO operating_hours (branch_id, day_of_week, open_time, close_time, is_closed) VALUES
(10,'MONDAY','07:00:00','23:00:00',FALSE),(10,'TUESDAY','07:00:00','23:00:00',FALSE),(10,'WEDNESDAY','07:00:00','23:00:00',FALSE),
(10,'THURSDAY','07:00:00','23:00:00',FALSE),(10,'FRIDAY','07:00:00','23:59:00',FALSE),(10,'SATURDAY','08:00:00','23:59:00',FALSE),(10,'SUNDAY','08:00:00','22:00:00',FALSE),
(11,'MONDAY','10:00:00','23:00:00',FALSE),(11,'TUESDAY','10:00:00','23:00:00',FALSE),(11,'WEDNESDAY','10:00:00','23:00:00',FALSE),
(11,'THURSDAY','10:00:00','23:00:00',FALSE),(11,'FRIDAY','10:00:00','23:59:00',FALSE),(11,'SATURDAY','10:00:00','23:59:00',FALSE),(11,'SUNDAY','10:00:00','22:00:00',FALSE),
(12,'MONDAY','09:00:00','23:00:00',FALSE),(12,'TUESDAY','09:00:00','23:00:00',FALSE),(12,'WEDNESDAY','09:00:00','23:00:00',FALSE),
(12,'THURSDAY','09:00:00','23:00:00',FALSE),(12,'FRIDAY','09:00:00','23:59:00',FALSE),(12,'SATURDAY','09:00:00','23:59:00',FALSE),(12,'SUNDAY','10:00:00','22:00:00',FALSE),
(13,'MONDAY','06:00:00','22:00:00',FALSE),(13,'TUESDAY','06:00:00','22:00:00',FALSE),(13,'WEDNESDAY','06:00:00','22:00:00',FALSE),
(13,'THURSDAY','06:00:00','22:00:00',FALSE),(13,'FRIDAY','06:00:00','22:00:00',FALSE),(13,'SATURDAY','07:00:00','22:00:00',FALSE),(13,'SUNDAY','07:00:00','21:00:00',FALSE),
(14,'MONDAY','11:00:00','23:30:00',FALSE),(14,'TUESDAY','11:00:00','23:30:00',FALSE),(14,'WEDNESDAY','11:00:00','23:30:00',FALSE),
(14,'THURSDAY','11:00:00','23:30:00',FALSE),(14,'FRIDAY','11:00:00','00:00:00',FALSE),(14,'SATURDAY','11:00:00','00:00:00',FALSE),(14,'SUNDAY','11:00:00','23:00:00',FALSE);

-- Staff assignments
INSERT INTO staff_branch_assignments (user_id, branch_id, designation) VALUES
(2, 10, 'General Manager'), (3, 10, 'Head Waiter'), (4, 10, 'Executive Chef');

-- Dining Tables — Branch 10 (MG Road)
INSERT INTO dining_tables (id, branch_id, table_number, seating_capacity, table_type, section, is_accessible, deposit_amount, is_active) VALUES
(10, 10, 'T-01', 2, 'STANDARD', 'ROOFTOP', TRUE, 200.00, TRUE),
(11, 10, 'T-02', 4, 'BOOTH', 'ROOFTOP', TRUE, 300.00, TRUE),
(12, 10, 'T-03', 6, 'STANDARD', 'MAIN_DINING', TRUE, 400.00, TRUE),
(13, 10, 'T-04', 8, 'HIGH_TOP', 'PATIO', TRUE, 500.00, TRUE),
(14, 10, 'T-05', 2, 'STANDARD', 'WINDOW', TRUE, 200.00, TRUE);

-- Dining Tables — Branch 11 (Koramangala)
INSERT INTO dining_tables (id, branch_id, table_number, seating_capacity, table_type, section, is_accessible, deposit_amount, is_active) VALUES
(15, 11, 'K-01', 2, 'STANDARD', 'MAIN_DINING', TRUE, 150.00, TRUE),
(16, 11, 'K-02', 4, 'BOOTH', 'MAIN_DINING', TRUE, 250.00, TRUE),
(17, 11, 'K-03', 6, 'STANDARD', 'WINDOW', TRUE, 350.00, TRUE);

-- Dining Tables — Branch 12 (Indiranagar)
INSERT INTO dining_tables (id, branch_id, table_number, seating_capacity, table_type, section, is_accessible, deposit_amount, is_active) VALUES
(18, 12, 'I-01', 2, 'STANDARD', 'MAIN_DINING', TRUE, 150.00, TRUE),
(19, 12, 'I-02', 4, 'BOOTH', 'PATIO', TRUE, 250.00, TRUE),
(20, 12, 'I-03', 6, 'STANDARD', 'MAIN_DINING', TRUE, 350.00, TRUE);

-- Dining Rooms
INSERT INTO dining_rooms (id, branch_id, room_name, capacity, description, amenities, base_price, hourly_price, deposit_amount, min_duration_hours, max_duration_hours, image_url, is_active) VALUES
(10, 10, 'Maharaja Suite', 14, 'Royal private dining suite with panoramic Bangalore city views, dedicated butler service and curated live ghazal music.', '4K Projector, Bose Sound, Personal Butler, Wine Chiller, AC', 5000.00, 2000.00, 2500.00, 2, 6, 'https://images.unsplash.com/photo-1544025162-d76694265947?w=800', TRUE),
(11, 11, 'Nawab Room', 10, 'Elegant biryani banquet room with Mughal decor for corporate lunches and family celebrations.', 'Conference Screen, Acoustic Walls, Private Restroom, AC', 3500.00, 1500.00, 1500.00, 1, 5, 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800', TRUE);

-- Party Halls
INSERT INTO party_halls (id, branch_id, hall_name, capacity, description, base_rental_price, hourly_rate, deposit_amount, min_duration_hours, max_duration_hours, image_url, is_active) VALUES
(10, 10, 'Rajdhani Grand Ballroom', 200, 'Luxurious ballroom for weddings, sangeet nights, corporate galas, and milestone celebrations with full event coordination team.', 50000.00, 15000.00, 25000.00, 4, 12, 'https://images.unsplash.com/photo-1519167758481-83f550bb49b3?w=800', TRUE),
(11, 12, 'Punjabi Celebration Hall', 100, 'Vibrant hall perfect for roka ceremonies, birthday bashes, baby showers and family reunions with live dhol and folk music arrangements.', 30000.00, 8000.00, 15000.00, 3, 10, 'https://images.unsplash.com/photo-1464366400600-7168b8af9bc3?w=800', TRUE);

INSERT INTO hall_packages (id, hall_id, package_name, price_per_guest, description, is_active) VALUES
(10, 10, 'Silver Celebration Package', 800.00, 'Welcome drinks, 5-item appetizer spread, 3-course dinner buffet, dessert counter and professional event MC.', TRUE),
(11, 10, 'Platinum Royal Wedding Package', 1500.00, 'Full-day event with brunch + dinner, unlimited soft drinks, live ghazal band, phoolon ki chaadar entry, customized wedding cake and floral decor.', TRUE),
(12, 11, 'Corporate Lunch Package', 600.00, 'Executive lunch buffet, 2-hour slot, projector setup, branded table menu cards and welcome chai.', TRUE);

-- Parking Slots
INSERT INTO parking_slots (id, branch_id, slot_number, vehicle_type, hourly_rate, is_covered, is_accessible, is_active) VALUES
(10, 10, 'P-A01', 'CAR', 60.00, TRUE, FALSE, TRUE),
(11, 10, 'P-A02', 'CAR', 60.00, TRUE, FALSE, TRUE),
(12, 10, 'P-A03', 'EV', 80.00, TRUE, TRUE, TRUE),
(13, 10, 'P-B01', 'MOTORCYCLE', 30.00, TRUE, FALSE, TRUE),
(14, 11, 'P-K01', 'CAR', 50.00, TRUE, FALSE, TRUE),
(15, 11, 'P-K02', 'CAR', 50.00, FALSE, FALSE, TRUE);

-- Menu Categories
INSERT INTO menu_categories (id, name, display_order, is_active) VALUES
(10, 'Starters & Chaats', 10, TRUE),
(11, 'Biryani & Rice Dishes', 11, TRUE),
(12, 'Curries & Gravies', 12, TRUE),
(13, 'Breads & Tandoor', 13, TRUE),
(14, 'South Indian Tiffin', 14, TRUE),
(15, 'Continental & Fusion', 15, TRUE),
(16, 'Desserts & Mithai', 16, TRUE),
(17, 'Beverages & Drinks', 17, TRUE);

-- Menu Items — Branch 10 (MG Road — Spice Garden)
INSERT INTO menu_items (id, branch_id, category_id, name, description, base_price, image_url, is_vegetarian, is_vegan, is_gluten_free, allergen_info, prep_time_minutes, tax_rate_percent, is_available) VALUES
(100, 10, 10, 'Pani Puri (8 pcs)', 'Crispy semolina puris filled with spiced tangy tamarind water, mint chutney, and masala potatoes. Bangalore street food classic.', 80.00, 'https://images.unsplash.com/photo-1606491956689-2ea866880c84?w=800', TRUE, TRUE, FALSE, 'Gluten', 5, 5.00, TRUE),
(101, 10, 10, 'Veg Spring Rolls (6 pcs)', 'Crispy golden rolls stuffed with seasoned cabbage, carrots, bell peppers, and glass noodles with red chili dipping sauce.', 180.00, 'https://images.unsplash.com/photo-1548802673-380ab8ebc7b7?w=800', TRUE, TRUE, FALSE, 'Gluten, Sesame', 12, 5.00, TRUE),
(102, 10, 10, 'Chicken 65', 'Spicy deep-fried chicken marinated with yogurt, red chilies, curry leaves, and mustard seeds. South Indian bar-bites legend.', 280.00, 'https://images.unsplash.com/photo-1610057099431-d73a1c9d2f2f?w=800', FALSE, FALSE, TRUE, 'Dairy', 18, 5.00, TRUE),
(103, 10, 11, 'Chicken Dum Biryani', 'Slow-cooked Hyderabadi style dum biryani with free-range chicken, saffron basmati rice, caramelized onions and mint raita.', 320.00, 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800', FALSE, FALSE, TRUE, 'Dairy', 30, 5.00, TRUE),
(104, 10, 11, 'Veg Dum Biryani', 'Aromatic basmati rice slow-cooked with mixed vegetables, whole spices, saffron, crispy onions and fresh mint in sealed handi.', 240.00, 'https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=800', TRUE, FALSE, TRUE, 'Dairy', 25, 5.00, TRUE),
(105, 10, 12, 'Butter Chicken (Murgh Makhani)', 'Tender tandoori chicken simmered in a rich, velvety tomato-butter-cream gravy with aromatic spices. Best paired with naan.', 380.00, 'https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=800', FALSE, FALSE, TRUE, 'Dairy', 20, 5.00, TRUE),
(106, 10, 12, 'Paneer Tikka Masala', 'Chargrilled cottage cheese cubes in spiced onion-tomato gravy with fenugreek and cream. Vegetarian showstopper.', 290.00, 'https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=800', TRUE, FALSE, TRUE, 'Dairy', 18, 5.00, TRUE),
(107, 10, 13, 'Butter Naan (2 pcs)', 'Soft leavened flatbread baked in clay tandoor, brushed with butter and garlic. Perfect with any curry.', 60.00, 'https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=800', TRUE, FALSE, FALSE, 'Gluten, Dairy', 8, 5.00, TRUE),
(108, 10, 16, 'Gulab Jamun (3 pcs)', 'Soft milk-solid dumplings soaked in rose-flavored sugar syrup, served warm with vanilla ice cream.', 120.00, 'https://images.unsplash.com/photo-1601050690597-df0568f70950?w=800', TRUE, FALSE, FALSE, 'Dairy, Gluten', 10, 5.00, TRUE),
(109, 10, 17, 'Mango Lassi', 'Thick, chilled yogurt-based drink blended with Alphonso mango pulp, cardamom, and a pinch of saffron.', 90.00, 'https://images.unsplash.com/photo-1553361371-9b22f78e8b1d?w=800', TRUE, FALSE, TRUE, 'Dairy', 3, 5.00, TRUE);

-- Menu Items — Branch 11 (Koramangala — Biryani House)
INSERT INTO menu_items (id, branch_id, category_id, name, description, base_price, image_url, is_vegetarian, is_vegan, is_gluten_free, allergen_info, prep_time_minutes, tax_rate_percent, is_available) VALUES
(110, 11, 10, 'Shammi Kebab (4 pcs)', 'Minced mutton patties mixed with lentils, aromatic spices, and pan-fried to perfection. Served with mint chutney.', 220.00, 'https://images.unsplash.com/photo-1599487488170-d11ec9c172f0?w=800', FALSE, FALSE, TRUE, 'None', 15, 5.00, TRUE),
(111, 11, 11, 'Mutton Dum Biryani', 'Royal Lucknowi style mutton biryani with tender slow-cooked mutton, kewra water, saffron basmati and golden fried onions.', 420.00, 'https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800', FALSE, FALSE, TRUE, 'Dairy', 45, 5.00, TRUE),
(112, 11, 11, 'Prawn Biryani', 'Juicy tiger prawns cooked with fragrant basmati rice, coastal spices, coconut and fresh coriander in sealed pot.', 450.00, 'https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=800', FALSE, FALSE, TRUE, 'Seafood, Dairy', 35, 5.00, TRUE),
(113, 11, 12, 'Mutton Rogan Josh', 'Kashmiri style slow-braised mutton in aromatic red gravy with whole spices, dried flowers and saffron cream.', 480.00, 'https://images.unsplash.com/photo-1574484284002-952d92456975?w=800', FALSE, FALSE, TRUE, 'Dairy', 35, 5.00, TRUE),
(114, 11, 17, 'Special Biryani Raita', 'House-made boondi raita with fresh coriander, roasted cumin and pomegranate seeds.', 60.00, 'https://images.unsplash.com/photo-1553361371-9b22f78e8b1d?w=800', TRUE, FALSE, TRUE, 'Dairy', 3, 5.00, TRUE);

-- Menu Items — Branch 12 (Indiranagar — Punjabi Dhaba)
INSERT INTO menu_items (id, branch_id, category_id, name, description, base_price, image_url, is_vegetarian, is_vegan, is_gluten_free, allergen_info, prep_time_minutes, tax_rate_percent, is_available) VALUES
(120, 12, 10, 'Amritsari Fish Tikka', 'Fresh Basa fish marinated in ajwain-spiced yogurt batter, pan-cooked on tawa. Served with green chutney and lemon.', 320.00, 'https://images.unsplash.com/photo-1599488615731-7e5c2823ff28?w=800', FALSE, FALSE, TRUE, 'Seafood, Dairy', 20, 5.00, TRUE),
(121, 12, 12, 'Dal Makhani', '18-hour slow-cooked black lentils with butter, cream, tomatoes and smoky tandoor flavor. The Dhaba legend.', 220.00, 'https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=800', TRUE, FALSE, TRUE, 'Dairy', 15, 5.00, TRUE),
(122, 12, 12, 'Sarson da Saag + Makki di Roti', 'Classic Punjabi mustard greens cooked with spinach, ginger and white butter, served with cornmeal flatbread.', 260.00, 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=800', TRUE, TRUE, FALSE, 'Gluten', 20, 5.00, TRUE),
(123, 12, 13, 'Stuffed Paratha (2 pcs)', 'Wheat flatbread stuffed with spiced aloo or paneer, griddled with white butter, served with pickle and curd.', 140.00, 'https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=800', TRUE, FALSE, FALSE, 'Gluten, Dairy', 12, 5.00, TRUE),
(124, 12, 17, 'Masala Chai (Pot)', 'Strong Assam tea brewed with ginger, cardamom, cinnamon and fresh milk. Served piping hot in earthen kulhad.', 50.00, 'https://images.unsplash.com/photo-1571934811356-5cc061b6821f?w=800', TRUE, FALSE, TRUE, 'Dairy', 5, 5.00, TRUE);

-- Menu Items — Branch 13 (Whitefield — South Indian Mess)
INSERT INTO menu_items (id, branch_id, category_id, name, description, base_price, image_url, is_vegetarian, is_vegan, is_gluten_free, allergen_info, prep_time_minutes, tax_rate_percent, is_available) VALUES
(130, 13, 14, 'Masala Dosa', 'Crispy golden rice-lentil crepe filled with spiced potato masala, served with coconut chutney and sambar. Bengaluru morning staple.', 80.00, 'https://images.unsplash.com/photo-1630383249896-424e482df921?w=800', TRUE, TRUE, TRUE, 'None', 10, 5.00, TRUE),
(131, 13, 14, 'Idli Sambar (4 pcs)', 'Steamed soft rice cakes served with piping hot toor dal sambar and three chutneys — coconut, tomato and onion.', 60.00, 'https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=800', TRUE, TRUE, TRUE, 'None', 5, 5.00, TRUE),
(132, 13, 14, 'Pesarattu', 'Crispy green moong dal crepe topped with ginger, green chilies and onions, served with ginger chutney.', 90.00, 'https://images.unsplash.com/photo-1630383249896-424e482df921?w=800', TRUE, TRUE, TRUE, 'None', 10, 5.00, TRUE),
(133, 13, 14, 'Full South Indian Thali', 'Complete meal with steamed rice, sambar, rasam, 3 vegetable curries, papad, pickle, payasam and unlimited curd rice.', 180.00, 'https://images.unsplash.com/photo-1585937421612-70a008356fbe?w=800', TRUE, FALSE, TRUE, 'Dairy', 15, 5.00, TRUE),
(134, 13, 17, 'Filter Coffee', 'Strong Coorg arabica decoction mixed with hot frothy milk, served in traditional stainless steel davara-tumbler set.', 30.00, 'https://images.unsplash.com/photo-1571934811356-5cc061b6821f?w=800', TRUE, FALSE, TRUE, 'Dairy', 3, 5.00, TRUE);

-- Menu Items — Branch 14 (HSR Layout — Social Table)
INSERT INTO menu_items (id, branch_id, category_id, name, description, base_price, image_url, is_vegetarian, is_vegan, is_gluten_free, allergen_info, prep_time_minutes, tax_rate_percent, is_available) VALUES
(140, 14, 10, 'Nachos with Salsa (Loaded)', 'Crispy corn tortilla chips loaded with three-cheese blend, jalapeños, sour cream, and house-made pico de gallo.', 280.00, 'https://images.unsplash.com/photo-1582169296194-e4d644c48063?w=800', TRUE, FALSE, TRUE, 'Dairy, Gluten', 10, 5.00, TRUE),
(141, 14, 15, 'Wood-Fired Margherita Pizza', 'San Marzano tomato base, fresh buffalo mozzarella, basil and extra virgin olive oil. Fired to perfection in our custom wood oven.', 450.00, 'https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=800', TRUE, FALSE, FALSE, 'Gluten, Dairy', 15, 5.00, TRUE),
(142, 14, 15, 'The HSR Smash Burger', 'Juicy double smash patty, American cheese, caramelized onions, house pickles, special sauce in toasted brioche bun.', 380.00, 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=800', FALSE, FALSE, FALSE, 'Gluten, Dairy', 15, 5.00, TRUE),
(143, 14, 15, 'Truffle Parmesan Fries', 'Crispy Belgian frites tossed in truffle oil, grated Parmesan, parsley and served with garlic aioli.', 220.00, 'https://images.unsplash.com/photo-1541592106381-b31e9677c0e5?w=800', TRUE, FALSE, TRUE, 'Dairy', 12, 5.00, TRUE),
(144, 14, 16, 'Belgian Chocolate Lava Cake', 'Warm dark chocolate fondant with molten center, served with vanilla bean ice cream and berry compote.', 280.00, 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=800', TRUE, FALSE, FALSE, 'Dairy, Eggs, Gluten', 15, 5.00, TRUE),
(145, 14, 17, 'Frozen Margarita', 'Classic blended lime-tequila margarita with salted rim and fresh lime wedge. Bangalore party crowd favourite.', 350.00, 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=800', TRUE, TRUE, TRUE, 'Alcohol', 5, 5.00, TRUE);

-- Variants
INSERT INTO menu_item_variants (id, menu_item_id, variant_name, price_delta) VALUES
(100, 141, 'Regular (10 inch)', 0.00),
(101, 141, 'Large (14 inch)', 120.00),
(102, 142, 'Single Patty', 0.00),
(103, 142, 'Double Smash Patty', 100.00),
(104, 103, 'Half Plate', 0.00),
(105, 103, 'Full Plate (Extra Rice)', 80.00),
(106, 111, 'Half Plate', 0.00),
(107, 111, 'Full Plate', 100.00);

-- Addons
INSERT INTO menu_item_addons (id, menu_item_id, addon_name, price) VALUES
(100, 105, 'Extra Butter Gravy', 40.00),
(101, 105, 'Add Naan', 30.00),
(102, 141, 'Extra Cheese', 60.00),
(103, 141, 'Mushroom Topping', 80.00),
(104, 142, 'Add Bacon Strips', 80.00),
(105, 142, 'Extra Patty', 100.00),
(106, 133, 'Add Ghee Rice', 50.00),
(107, 103, 'Add Extra Raita', 30.00);

-- Inventory
INSERT INTO inventory_items (id, menu_item_id, quantity_in_stock, low_stock_threshold) VALUES
(100,100,80,15),(101,101,60,10),(102,102,45,8),(103,103,40,8),(104,104,50,10),
(105,105,35,8),(106,106,40,8),(107,107,80,15),(108,108,30,5),(109,109,60,10),
(110,110,30,5),(111,111,25,5),(112,112,20,5),(113,113,20,5),(114,114,80,20),
(120,120,30,5),(121,121,50,10),(122,122,40,8),(123,123,60,10),(124,124,100,20),
(130,130,100,20),(131,131,120,20),(132,132,80,15),(133,133,60,10),(134,134,150,30),
(140,140,50,10),(141,141,40,8),(142,142,35,8),(143,143,60,10),(144,144,25,5),(145,145,40,8);

-- Update coupons for INR
UPDATE coupons SET min_order_amount = 299.00, max_discount_amount = 100.00 WHERE code = 'WELCOME20';
UPDATE coupons SET discount_value = 50.00, min_order_amount = 399.00, max_discount_amount = 50.00, code = 'FLAT50' WHERE code = 'FLAT10';

-- Add Bangalore-specific coupons
INSERT INTO coupons (id, code, discount_type, discount_value, min_order_amount, max_discount_amount, usage_limit, used_count, valid_from, valid_until, is_active) VALUES
(3, 'BENGALURU10', 'PERCENTAGE', 10.00, 199.00, 80.00, 2000, 0, '2025-01-01 00:00:00', '2027-12-31 23:59:59', TRUE),
(4, 'NEWUSER100', 'FIXED_AMOUNT', 100.00, 499.00, 100.00, 500, 0, '2025-01-01 00:00:00', '2027-12-31 23:59:59', TRUE);
