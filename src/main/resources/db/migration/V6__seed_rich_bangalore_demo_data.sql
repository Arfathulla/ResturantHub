-- =====================================================================
-- Bhojana Bangalore Flyway Migration V6: Seed Rich Bangalore Demo Data
-- Seed comprehensive live data for Food Orders, Delivery Tracking,
-- Dining Reservations, Private Rooms, Party Halls, Parking & Payments
-- =====================================================================

-- 1. Demo Food Orders
INSERT INTO orders (id, order_number, user_id, branch_id, order_type, status, delivery_address, subtotal, tax_amount, discount_amount, delivery_fee, final_total, payment_method, special_instructions, created_at, updated_at) VALUES
(1001, 'ORD-20261103-1001', 6, 10, 'DELIVERY', 'COMPLETED', 'Flat 402, Prestige Tower, Residency Road, MG Road, Bengaluru 560025', 680.00, 34.00, 0.00, 40.00, 754.00, 'UPI', 'Please ring the doorbell and leave at door', '2026-11-03 12:30:00', '2026-11-03 13:15:00'),
(1002, 'ORD-20261104-1002', 5, 11, 'DELIVERY', 'OUT_FOR_DELIVERY', 'Villa 12, Palm Meadows, 4th Block, Koramangala, Bengaluru 560034', 840.00, 42.00, 50.00, 50.00, 882.00, 'CARD', 'Extra mint raita requested', '2026-11-04 19:15:00', '2026-11-04 19:50:00'),
(1003, 'ORD-20261105-1003', 6, 12, 'DELIVERY', 'PREPARING', 'House #55, 12th Main, 100 Feet Road, Indiranagar, Bengaluru 560038', 540.00, 27.00, 0.00, 40.00, 607.00, 'UPI', 'Make curries medium spicy please', '2026-11-05 20:00:00', '2026-11-05 20:15:00'),
(1004, 'ORD-20261106-1004', 5, 13, 'DELIVERY', 'ACCEPTED', 'Tower 3, ITPL Tech Corridor, Whitefield, Bengaluru 560066', 350.00, 17.50, 0.00, 30.00, 397.50, 'UPI', 'Include extra filter coffee decoction', '2026-11-06 08:30:00', '2026-11-06 08:35:00'),
(1005, 'ORD-20261107-1005', 6, 14, 'DELIVERY', 'PLACED', 'Sector 3, 27th Main Road, HSR Layout, Bengaluru 560102', 730.00, 36.50, 0.00, 40.00, 806.50, 'CARD', 'Hot and crispy truffle fries please', '2026-11-07 10:45:00', '2026-11-07 10:45:00')
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 2. Demo Order Items
INSERT INTO order_items (id, order_id, menu_item_id, item_name_snapshot, unit_price_snapshot, quantity, total_price, special_instructions) VALUES
(2001, 1001, 103, 'Chicken Dum Biryani', 320.00, 1, 320.00, 'Served with salan and raita'),
(2002, 1001, 105, 'Butter Chicken (Murgh Makhani)', 380.00, 1, 380.00, 'Rich tomato butter gravy'),
(2003, 1001, 107, 'Butter Naan (2 pcs)', 60.00, 1, 60.00, 'Crispy garlic butter naan'),
(2004, 1002, 111, 'Mutton Dum Biryani', 420.00, 1, 420.00, 'Royal Lucknowi style slow-cooked'),
(2005, 1002, 110, 'Shammi Kebab (4 pcs)', 220.00, 1, 220.00, 'Pan-fried mutton patties'),
(2006, 1002, 114, 'Special Biryani Raita', 60.00, 1, 60.00, 'Boondi and pomegranate raita'),
(2007, 1003, 120, 'Amritsari Fish Tikka', 320.00, 1, 320.00, 'Ajwain-spiced freshwater Basa'),
(2008, 1003, 121, 'Dal Makhani', 220.00, 1, 220.00, '18-hour slow cooked black lentils'),
(2009, 1004, 133, 'Full South Indian Thali', 180.00, 1, 180.00, 'Sambar, rasam, 3 curries and payasam'),
(2010, 1004, 130, 'Masala Dosa', 80.00, 1, 80.00, 'Crispy Bengaluru golden dosa'),
(2011, 1004, 134, 'Filter Coffee', 30.00, 3, 90.00, 'Coorg arabica hot filter coffee'),
(2012, 1005, 141, 'Wood-Fired Margherita Pizza', 450.00, 1, 450.00, 'Buffalo mozzarella & basil'),
(2013, 1005, 143, 'Truffle Parmesan Fries', 220.00, 1, 220.00, 'Truffle oil with garlic aioli'),
(2014, 1005, 109, 'Mango Lassi', 90.00, 1, 90.00, 'Alphonso mango lassi')
ON DUPLICATE KEY UPDATE quantity = VALUES(quantity);

-- 3. Demo Delivery Tracking
INSERT INTO delivery_tracking (id, order_id, status, current_location, driver_name, driver_phone, vehicle_number, estimated_delivery_minutes, status_updated_at, created_at, updated_at) VALUES
(3001, 1001, 'COMPLETED', 'Delivered at Customer Address (MG Road)', 'Kiran Gowda', '+91 99002 65432', 'KA-01-EQ-4921', 0, '2026-11-03 13:15:00', '2026-11-03 12:30:00', '2026-11-03 13:15:00'),
(3002, 1002, 'OUT_FOR_DELIVERY', 'Intermediate Ring Road near Domlur Flyover, Bengaluru', 'Manjunath Hegde', '+91 98450 12398', 'KA-05-MK-3312', 12, '2026-11-04 19:50:00', '2026-11-04 19:15:00', '2026-11-04 19:50:00'),
(3003, 1003, 'PREPARING', 'Kitchen preparing fresh order at Indiranagar Dhaba', 'Suresh Kumar', '+91 97410 77654', 'KA-03-TR-8819', 25, '2026-11-05 20:15:00', '2026-11-05 20:00:00', '2026-11-05 20:15:00'),
(3004, 1004, 'ACCEPTED', 'Order confirmed by South Indian Mess, Whitefield', 'Delivery Partner Assigned upon Packing', '+91 98801 23456', 'KA-04-AB-1234', 35, '2026-11-06 08:35:00', '2026-11-06 08:30:00', '2026-11-06 08:35:00'),
(3005, 1005, 'PLACED', 'Order placed, awaiting kitchen confirmation at HSR Layout', 'Partner to be assigned', '+91 98800 00000', 'KA-05-XX-0000', 45, '2026-11-07 10:45:00', '2026-11-07 10:45:00', '2026-11-07 10:45:00')
ON DUPLICATE KEY UPDATE status = VALUES(status), current_location = VALUES(current_location);

-- 4. Demo Order Status History
INSERT INTO order_status_history (id, order_id, from_status, to_status, changed_by_user_id, comment, changed_at) VALUES
(4001, 1001, 'PENDING_PAYMENT', 'PLACED', 6, 'Order successfully placed via online cart', '2026-11-03 12:30:00'),
(4002, 1001, 'PLACED', 'ACCEPTED', 3, 'Restaurant confirmed order', '2026-11-03 12:35:00'),
(4003, 1001, 'ACCEPTED', 'PREPARING', 4, 'Executive chef started preparation', '2026-11-03 12:45:00'),
(4004, 1001, 'PREPARING', 'READY_FOR_PICKUP', 4, 'Order freshly packed in hot bags', '2026-11-03 13:00:00'),
(4005, 1001, 'READY_FOR_PICKUP', 'OUT_FOR_DELIVERY', 3, 'Handed over to rider Kiran Gowda', '2026-11-03 13:05:00'),
(4006, 1001, 'OUT_FOR_DELIVERY', 'COMPLETED', 3, 'Delivered to customer doorstep', '2026-11-03 13:15:00'),
(4007, 1002, 'PLACED', 'OUT_FOR_DELIVERY', 3, 'Order en-route with rider Manjunath Hegde', '2026-11-04 19:50:00'),
(4008, 1003, 'PLACED', 'PREPARING', 4, 'Tandoor cooking in progress', '2026-11-05 20:15:00')
ON DUPLICATE KEY UPDATE comment = VALUES(comment);

-- 5. Demo Table Reservations
INSERT INTO table_reservations (id, reference_no, user_id, branch_id, table_id, reservation_date, start_time, end_time, guest_count, status, special_requests, deposit_amount, deposit_paid, created_at) VALUES
(1001, 'TR-20261105-001', 6, 10, 10, '2026-11-10', '19:30:00', '21:30:00', 2, 'CONFIRMED', 'Rooftop table with city view for anniversary dinner', 200.00, TRUE, '2026-11-05 14:00:00'),
(1002, 'TR-20261106-002', 5, 11, 16, '2026-11-11', '13:00:00', '15:00:00', 4, 'CONFIRMED', 'Family lunch near window section', 250.00, TRUE, '2026-11-06 10:30:00'),
(1003, 'TR-20261107-003', 6, 12, 19, '2026-11-12', '20:00:00', '22:00:00', 4, 'CONFIRMED', 'Patio seating with high chairs for kids', 250.00, TRUE, '2026-11-07 11:00:00'),
(1004, 'TR-20261108-004', 5, 10, 12, '2026-11-13', '19:00:00', '21:00:00', 6, 'SEATED', 'Business dinner table', 400.00, TRUE, '2026-11-07 11:15:00')
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- 6. Demo Private Dining Room Bookings
INSERT INTO room_reservations (id, reference_no, user_id, branch_id, room_id, reservation_date, start_time, end_time, guest_count, duration_hours, total_price, deposit_amount, status, special_requests, created_at) VALUES
(1001, 'RR-20261105-101', 6, 10, 10, '2026-11-15', '19:00:00', '22:00:00', 10, 3, 11000.00, 2500.00, 'CONFIRMED', 'Maharaja Suite VIP dining with personal butler and custom floral setup', '2026-11-05 16:30:00'),
(1002, 'RR-20261107-102', 5, 11, 11, '2026-11-16', '13:00:00', '15:00:00', 8, 2, 6500.00, 1500.00, 'CONFIRMED', 'Nawab Room corporate luncheon with conference screen', '2026-11-07 09:45:00')
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- 7. Demo Party Hall & Banquet Bookings
INSERT INTO hall_reservations (id, reference_no, user_id, branch_id, hall_id, package_id, event_type, reservation_date, start_time, end_time, guest_count, duration_hours, hall_rental_charge, package_charge, total_price, deposit_amount, status, special_requests, created_at) VALUES
(1001, 'HR-20261110-201', 6, 10, 10, 10, 'Corporate Gala & Tech Awards Night', '2026-11-20', '18:00:00', '23:00:00', 120, 5, 125000.00, 96000.00, 221000.00, 25000.00, 'CONFIRMED', 'Rajdhani Grand Ballroom with live acoustic band setup and 4K projectors', '2026-11-05 18:00:00'),
(1002, 'HR-20261115-202', 5, 12, 11, 12, 'Family Reunion & Sangeet Evening', '2026-11-25', '12:00:00', '16:00:00', 75, 4, 62000.00, 45000.00, 107000.00, 15000.00, 'CONFIRMED', 'Punjabi Celebration Hall with traditional dhol welcome and live buffet', '2026-11-07 10:15:00')
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- 8. Demo Parking Reservations
INSERT INTO parking_reservations (id, reference_no, user_id, branch_id, parking_slot_id, vehicle_registration_no, vehicle_type, reservation_date, start_time, end_time, total_price, status, created_at) VALUES
(1001, 'PR-20261105-301', 6, 10, 10, 'KA-01-MJ-2024', 'CAR', '2026-11-10', '19:00:00', '22:00:00', 180.00, 'CONFIRMED', '2026-11-05 14:10:00'),
(1002, 'PR-20261106-302', 5, 10, 12, 'KA-05-EV-1001', 'EV', '2026-11-13', '18:30:00', '21:30:00', 240.00, 'CONFIRMED', '2026-11-06 10:45:00'),
(1003, 'PR-20261107-303', 6, 11, 14, 'KA-03-NB-8899', 'CAR', '2026-11-04', '19:00:00', '22:00:00', 150.00, 'COMPLETED', '2026-11-04 18:30:00')
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- 9. Demo Payments
INSERT INTO payments (id, payment_reference, user_id, order_id, table_reservation_id, room_reservation_id, hall_reservation_id, parking_reservation_id, amount, currency, provider, gateway_transaction_id, status, idempotency_key, payment_method, payment_details, created_at) VALUES
(1001, 'PAY-20261103-901', 6, 1001, NULL, NULL, NULL, NULL, 754.00, 'INR', 'MOCK_RAZORPAY', 'pay_blr_901_upi_sbi', 'SUCCESS', 'IDEMP-ORD-1001', 'UPI', 'Google Pay / arfath@oksbi', '2026-11-03 12:30:05'),
(1002, 'PAY-20261104-902', 5, 1002, NULL, NULL, NULL, NULL, 882.00, 'INR', 'MOCK_RAZORPAY', 'pay_blr_902_card_hdfc', 'SUCCESS', 'IDEMP-ORD-1002', 'CARD', 'HDFC Visa ending 4921', '2026-11-04 19:15:10'),
(1003, 'PAY-20261105-903', 6, 1003, NULL, NULL, NULL, NULL, 607.00, 'INR', 'MOCK_RAZORPAY', 'pay_blr_903_upi_icici', 'SUCCESS', 'IDEMP-ORD-1003', 'UPI', 'PhonePe / arfath@ybl', '2026-11-05 20:00:05'),
(1004, 'PAY-20261106-904', 5, 1004, NULL, NULL, NULL, NULL, 397.50, 'INR', 'MOCK_RAZORPAY', 'pay_blr_904_upi_axis', 'SUCCESS', 'IDEMP-ORD-1004', 'UPI', 'Paytm UPI / alice@paytm', '2026-11-06 08:30:05'),
(1005, 'PAY-20261107-905', 6, 1005, NULL, NULL, NULL, NULL, 806.50, 'INR', 'MOCK_RAZORPAY', 'pay_blr_905_card_sbi', 'SUCCESS', 'IDEMP-ORD-1005', 'CARD', 'SBI Mastercard ending 8812', '2026-11-07 10:45:05'),
(1006, 'PAY-20261105-906', 6, NULL, 1001, NULL, NULL, NULL, 200.00, 'INR', 'MOCK_RAZORPAY', 'pay_blr_906_table_dep', 'SUCCESS', 'IDEMP-TR-1001', 'UPI', 'Table Reservation Deposit', '2026-11-05 14:00:05'),
(1007, 'PAY-20261105-907', 6, NULL, NULL, 1001, NULL, NULL, 2500.00, 'INR', 'MOCK_RAZORPAY', 'pay_blr_907_room_dep', 'SUCCESS', 'IDEMP-RR-1001', 'UPI', 'Maharaja Suite Deposit', '2026-11-05 16:30:05'),
(1008, 'PAY-20261105-908', 6, NULL, NULL, NULL, 1001, NULL, 25000.00, 'INR', 'MOCK_RAZORPAY', 'pay_blr_908_hall_dep', 'SUCCESS', 'IDEMP-HR-1001', 'NET_BANKING', 'Ballroom Booking Deposit', '2026-11-05 18:00:05')
ON DUPLICATE KEY UPDATE status = VALUES(status);
