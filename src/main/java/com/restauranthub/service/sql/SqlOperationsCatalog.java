package com.restauranthub.service.sql;

import com.restauranthub.dto.sql.SqlOperationDef;

import java.util.*;

public final class SqlOperationsCatalog {

    private static final List<SqlOperationDef> ALL_OPERATIONS = new ArrayList<>();
    private static final Map<String, SqlOperationDef> BY_KEY = new LinkedHashMap<>();

    private SqlOperationsCatalog() {}

    private static void register(int number, String key, String name, String category,
                                 String targetEntity, String description, String sql) {
        SqlOperationDef def = SqlOperationDef.builder()
                .number(number)
                .key(key)
                .name(name)
                .category(category)
                .targetEntity(targetEntity)
                .description(description)
                .sql(sql)
                .build();
        ALL_OPERATIONS.add(def);
        BY_KEY.put(key, def);
    }

    static {
        final String CAT_BASIC = "Basic SQL Operations";
        final String CAT_INTERMEDIATE = "Intermediate SQL Operations";
        final String CAT_ADVANCED = "Advanced SQL Operations";

        // =====================================================================
        // Basic SQL Operations (1 - 18)
        // =====================================================================
        register(1, "op_01_create_schema",
                "1. CREATE SCHEMA", CAT_BASIC,
                "SCHEMA / DATABASE bhojana_bangalore_db",
                "Creates or verifies existence of dedicated schema/database with UTF-8 character encoding.",
                "CREATE DATABASE IF NOT EXISTS bhojana_bangalore_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; SELECT schema_name AS schema_created, default_character_set_name, default_collation_name FROM information_schema.schemata WHERE schema_name = 'bhojana_bangalore_db';");

        register(2, "op_02_create_table",
                "2. CREATE TABLE", CAT_BASIC,
                "TABLE dining_table_audit",
                "Creates structured relational table with primary key, typed columns, and engine specifications.",
                "CREATE TABLE IF NOT EXISTS dining_table_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, table_number VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, guest_capacity INT DEFAULT 4, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB; DESCRIBE dining_table_audit;");

        register(3, "op_03_drop_schema",
                "3. DROP SCHEMA", CAT_BASIC,
                "SCHEMA demo_temp_sandbox",
                "Safely executes clean removal of a temporary schema/database partition.",
                "CREATE DATABASE IF NOT EXISTS demo_temp_sandbox; DROP SCHEMA IF EXISTS demo_temp_sandbox; SELECT 'Schema demo_temp_sandbox safely dropped' AS operation_status;");

        register(4, "op_04_drop_table",
                "4. DROP TABLE", CAT_BASIC,
                "TABLE temp_kitchen_staging",
                "Safely drops temporary staging table without affecting transactional core data.",
                "CREATE TABLE IF NOT EXISTS temp_kitchen_staging (id INT PRIMARY KEY, note VARCHAR(100)); DROP TABLE IF EXISTS temp_kitchen_staging; SELECT 'Table temp_kitchen_staging successfully dropped' AS operation_status;");

        register(5, "op_05_select",
                "5. SELECT", CAT_BASIC,
                "orders (Projected Attributes)",
                "Selects specific columnar attributes from transactional food orders table.",
                "SELECT order_number, order_type, status, final_total, payment_method, created_at FROM orders ORDER BY id DESC LIMIT 15;");

        register(6, "op_06_from",
                "6. FROM", CAT_BASIC,
                "restaurant_branches",
                "Designates primary relation source for entity scanning and attribute extraction.",
                "SELECT name AS branch_name, code AS branch_code, city, phone_number, is_active FROM restaurant_branches;");

        register(7, "op_07_where",
                "7. WHERE", CAT_BASIC,
                "orders (Conditional Filter: COMPLETED & UPI)",
                "Filters rows using Boolean conditions matching order status and digital payment method.",
                "SELECT order_number, order_type, status, final_total, payment_method FROM orders WHERE status = 'COMPLETED' AND payment_method = 'UPI';");

        register(8, "op_08_order_by",
                "8. ORDER BY", CAT_BASIC,
                "menu_items (Sorted by Price DESC, Prep Time ASC)",
                "Sorts dataset multi-directionally by price descending then preparation duration ascending.",
                "SELECT name AS item_name, base_price, prep_time_minutes, is_vegetarian FROM menu_items ORDER BY base_price DESC, prep_time_minutes ASC LIMIT 20;");

        register(9, "op_09_insert",
                "9. INSERT", CAT_BASIC,
                "dining_table_audit",
                "Inserts fresh audit entries into relational database table.",
                "CREATE TABLE IF NOT EXISTS dining_table_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, table_number VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, guest_capacity INT DEFAULT 4, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB; INSERT INTO dining_table_audit (table_number, status, guest_capacity) VALUES ('T-ROOFTOP-01', 'RESERVED', 6), ('T-PATIO-05', 'AVAILABLE', 4); SELECT * FROM dining_table_audit ORDER BY audit_id DESC LIMIT 5;");

        register(10, "op_10_delete",
                "10. DELETE", CAT_BASIC,
                "dining_table_audit",
                "Deletes targeted records satisfying a specific predicate from the table.",
                "CREATE TABLE IF NOT EXISTS dining_table_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, table_number VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, guest_capacity INT DEFAULT 4, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB; INSERT INTO dining_table_audit (table_number, status, guest_capacity) VALUES ('TEMP-T-99', 'CANCELLED', 2); DELETE FROM dining_table_audit WHERE table_number LIKE 'TEMP-%'; SELECT 'Deleted temporary audit records' AS outcome;");

        register(11, "op_11_update",
                "11. UPDATE", CAT_BASIC,
                "dining_table_audit",
                "Modifies state of existing tuples matching where criteria.",
                "CREATE TABLE IF NOT EXISTS dining_table_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, table_number VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, guest_capacity INT DEFAULT 4, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB; UPDATE dining_table_audit SET status = 'OCCUPIED' WHERE table_number = 'T-ROOFTOP-01'; SELECT audit_id, table_number, status, recorded_at FROM dining_table_audit WHERE table_number = 'T-ROOFTOP-01';");

        register(12, "op_12_alter_table",
                "12. ALTER TABLE", CAT_BASIC,
                "dining_table_audit",
                "Alters table structure by appending an operational metadata column.",
                "CREATE TABLE IF NOT EXISTS dining_table_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, table_number VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, guest_capacity INT DEFAULT 4, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB; ALTER TABLE dining_table_audit ADD COLUMN IF NOT EXISTS notes VARCHAR(255) DEFAULT 'Standard seating'; DESCRIBE dining_table_audit;");

        register(13, "op_13_primary_key",
                "13. PRIMARY KEY", CAT_BASIC,
                "information_schema.table_constraints",
                "Verifies and demonstrates primary key uniqueness enforcement and index linkage.",
                "SELECT table_name, constraint_name, constraint_type FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND constraint_type = 'PRIMARY KEY' ORDER BY table_name LIMIT 15;");

        register(14, "op_14_foreign_key",
                "14. FOREIGN KEY", CAT_BASIC,
                "information_schema.key_column_usage",
                "Inspects active foreign key referential integrity constraints across tables.",
                "SELECT constraint_name, table_name, column_name, referenced_table_name, referenced_column_name FROM information_schema.key_column_usage WHERE table_schema = DATABASE() AND referenced_table_name IS NOT NULL ORDER BY table_name LIMIT 15;");

        register(15, "op_15_unique",
                "15. UNIQUE", CAT_BASIC,
                "information_schema.table_constraints",
                "Inspects UNIQUE candidate key constraints ensuring unicity on emails, codes, and order numbers.",
                "SELECT table_name, constraint_name FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND constraint_type = 'UNIQUE' ORDER BY table_name LIMIT 15;");

        register(16, "op_16_not_null",
                "16. NOT NULL", CAT_BASIC,
                "orders (Column Constraints)",
                "Inspects mandatory non-nullable schema attributes on transactional entities.",
                "SELECT column_name, is_nullable, data_type, column_default FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND is_nullable = 'NO' LIMIT 15;");

        register(17, "op_17_check",
                "17. CHECK", CAT_BASIC,
                "menu_items & pricing checks",
                "Enforces domain range and value validity constraint predicates.",
                "SELECT id, name, base_price, prep_time_minutes, (base_price > 0 AND prep_time_minutes >= 0) AS passes_check_constraint FROM menu_items LIMIT 15;");

        register(18, "op_18_default",
                "18. DEFAULT", CAT_BASIC,
                "information_schema.columns (Default Value Definitions)",
                "Inspects default values configured across platform schema tables.",
                "SELECT table_name, column_name, column_default, data_type FROM information_schema.columns WHERE table_schema = DATABASE() AND column_default IS NOT NULL ORDER BY table_name, ordinal_position LIMIT 15;");

        // =====================================================================
        // Intermediate SQL Operations (19 - 42)
        // =====================================================================
        register(19, "op_19_nested_queries",
                "19. Nested Queries", CAT_INTERMEDIATE,
                "menu_items WHERE base_price > (SELECT AVG)",
                "Executes non-correlated nested subquery to isolate premium priced culinary offerings.",
                "SELECT id, name AS dish_name, base_price, is_vegetarian FROM menu_items WHERE base_price > (SELECT AVG(base_price) FROM menu_items) ORDER BY base_price DESC LIMIT 15;");

        register(20, "op_20_correlated_nested_queries",
                "20. Correlated Nested Queries", CAT_INTERMEDIATE,
                "menu_items (Compared to Category Average)",
                "Executes correlated subquery referencing outer row's category_id to find category leaders.",
                "SELECT m1.id, m1.name AS item_name, m1.category_id, m1.base_price FROM menu_items m1 WHERE m1.base_price >= (SELECT AVG(m2.base_price) FROM menu_items m2 WHERE m2.category_id = m1.category_id) ORDER BY m1.category_id, m1.base_price DESC LIMIT 20;");

        register(21, "op_21_in",
                "21. IN", CAT_INTERMEDIATE,
                "orders WHERE status IN (...) ",
                "Filters values against discrete enumeration of active kitchen and delivery states.",
                "SELECT order_number, order_type, status, final_total, created_at FROM orders WHERE status IN ('PLACED', 'ACCEPTED', 'PREPARING', 'READY_FOR_PICKUP', 'OUT_FOR_DELIVERY') ORDER BY id DESC LIMIT 15;");

        register(22, "op_22_exists",
                "22. EXISTS", CAT_INTERMEDIATE,
                "users WHERE EXISTS (SELECT 1 FROM orders)",
                "Validates row existence in dependent order relation with Boolean short-circuiting.",
                "SELECT u.id, u.email, CONCAT(u.first_name, ' ', u.last_name) AS customer_name FROM users u WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id) ORDER BY u.id ASC LIMIT 15;");

        register(23, "op_23_not_exists",
                "23. NOT EXISTS", CAT_INTERMEDIATE,
                "users WHERE NOT EXISTS (SELECT 1 FROM orders)",
                "Detects registered user accounts with zero recorded food order transactions.",
                "SELECT u.id, u.email, CONCAT(u.first_name, ' ', u.last_name) AS customer_name, u.created_at FROM users u WHERE NOT EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id) ORDER BY u.id ASC LIMIT 15;");

        register(24, "op_24_any",
                "24. ANY", CAT_INTERMEDIATE,
                "orders WHERE final_total > ANY (SELECT deposit)",
                "Evaluates comparative operator against ANY element returned by subquery.",
                "SELECT order_number, final_total, payment_method FROM orders WHERE final_total > ANY (SELECT deposit_amount FROM dining_tables WHERE deposit_amount > 0) ORDER BY final_total DESC LIMIT 15;");

        register(25, "op_25_all",
                "25. ALL", CAT_INTERMEDIATE,
                "orders WHERE final_total > ALL (SELECT subtotal)",
                "Evaluates comparative condition strictly exceeding ALL values returned by subquery.",
                "SELECT order_number, final_total, status FROM orders WHERE final_total > ALL (SELECT subtotal FROM orders WHERE order_type = 'DINE_IN' AND subtotal IS NOT NULL) LIMIT 15;");

        register(26, "op_26_join",
                "26. JOIN", CAT_INTERMEDIATE,
                "orders JOIN users",
                "Combines attributes from orders and users based on relational foreign key match.",
                "SELECT o.order_number, u.email AS customer_email, o.order_type, o.status, o.final_total FROM orders o JOIN users u ON o.user_id = u.id ORDER BY o.id DESC LIMIT 15;");

        register(27, "op_27_inner_join",
                "27. INNER JOIN", CAT_INTERMEDIATE,
                "orders INNER JOIN restaurant_branches",
                "Merges completed orders with branch coordinates and manager details in the backend.",
                "SELECT o.order_number, CONCAT(u.first_name, ' ', u.last_name) AS customer_name, b.name AS restaurant_name, b.city AS branch_locality, 'Ramesh Rao' AS branch_manager, b.phone_number AS manager_contact, GROUP_CONCAT(CONCAT(oi.item_name_snapshot, ' (', oi.quantity, 'x)') SEPARATOR ', ') AS dishes_ordered FROM orders o INNER JOIN restaurant_branches b ON o.branch_id = b.id INNER JOIN users u ON o.user_id = u.id LEFT JOIN order_items oi ON o.id = oi.order_id GROUP BY o.id, o.order_number, u.first_name, u.last_name, b.name, b.city, b.phone_number ORDER BY o.id DESC LIMIT 15;");

        register(28, "op_28_left_outer_join",
                "28. LEFT OUTER JOIN", CAT_INTERMEDIATE,
                "restaurant_branches LEFT OUTER JOIN orders",
                "Preserves all restaurant branches even if zero orders have been processed yet.",
                "SELECT b.code AS branch_code, b.name AS branch_name, b.city AS locality, COUNT(o.id) AS total_orders, COALESCE(SUM(o.final_total), 0.00) AS total_revenue_inr FROM restaurant_branches b LEFT OUTER JOIN orders o ON b.id = o.branch_id GROUP BY b.id, b.code, b.name, b.city ORDER BY total_orders DESC;");

        register(29, "op_29_right_outer_join",
                "29. RIGHT OUTER JOIN", CAT_INTERMEDIATE,
                "delivery_tracking RIGHT OUTER JOIN orders",
                "Guarantees every food order appears alongside its real-time GPS tracking partner.",
                "SELECT o.order_number, u.email AS customer_email, dt.driver_name, dt.vehicle_number, dt.current_location, dt.estimated_delivery_minutes, dt.status AS delivery_status FROM delivery_tracking dt RIGHT OUTER JOIN orders o ON dt.order_id = o.id JOIN users u ON o.user_id = u.id ORDER BY o.id DESC LIMIT 15;");

        register(30, "op_30_full_outer_join",
                "30. FULL OUTER JOIN", CAT_INTERMEDIATE,
                "branches FULL OUTER JOIN orders (UNION of LEFT & RIGHT)",
                "Full outer join relational synthesis combining unmatched rows from both participating tables.",
                "SELECT b.id AS branch_id, b.name AS branch_name, o.order_number, o.final_total FROM restaurant_branches b LEFT JOIN orders o ON b.id = o.branch_id UNION SELECT b.id AS branch_id, b.name AS branch_name, o.order_number, o.final_total FROM restaurant_branches b RIGHT JOIN orders o ON b.id = o.branch_id LIMIT 20;");

        register(31, "op_31_count",
                "31. COUNT()", CAT_INTERMEDIATE,
                "orders (Total Order Counts)",
                "Counts total records and conditional occurrences using SQL COUNT aggregate function.",
                "SELECT COUNT(*) AS total_orders, COUNT(DISTINCT user_id) AS unique_customers, COUNT(DISTINCT branch_id) AS branches_with_orders FROM orders;");

        register(32, "op_32_sum",
                "32. SUM()", CAT_INTERMEDIATE,
                "orders (Gross Financial Revenue)",
                "Calculates overall platform revenues, sales tax, and delivery fee gross sums.",
                "SELECT SUM(subtotal) AS gross_food_sales, SUM(tax_amount) AS total_tax_collected, SUM(delivery_fee) AS total_delivery_fees, SUM(final_total) AS net_revenue_inr FROM orders;");

        register(33, "op_33_avg",
                "33. AVG()", CAT_INTERMEDIATE,
                "orders & menu_items",
                "Computes arithmetic mean value for order transaction baskets and dish pricing.",
                "SELECT ROUND(AVG(final_total), 2) AS average_order_basket_value, ROUND(AVG(subtotal), 2) AS average_subtotal, (SELECT ROUND(AVG(base_price), 2) FROM menu_items) AS average_dish_price FROM orders;");

        register(34, "op_34_min",
                "34. MIN()", CAT_INTERMEDIATE,
                "orders & menu_items (Lowest Price Values)",
                "Finds lowest ticket order value and lowest priced delicacy in Bangalore.",
                "SELECT MIN(final_total) AS lowest_order_amount, (SELECT MIN(base_price) FROM menu_items) AS lowest_item_price, (SELECT MIN(hourly_rate) FROM parking_slots) AS lowest_parking_rate FROM orders;");

        register(35, "op_35_max",
                "35. MAX()", CAT_INTERMEDIATE,
                "orders & party_halls (Peak Values)",
                "Identifies peak order invoice and highest capacity banquet facility.",
                "SELECT MAX(final_total) AS peak_order_value, (SELECT MAX(capacity) FROM party_halls) AS peak_hall_capacity, (SELECT MAX(total_price) FROM hall_reservations) AS peak_event_booking FROM orders;");

        register(36, "op_36_group_by",
                "36. GROUP BY", CAT_INTERMEDIATE,
                "orders GROUP BY branch_id",
                "Calculates SQL COUNT, MIN, MAX, AVG, and SUM aggregates per restaurant outlet.",
                "SELECT b.name AS branch_name, COUNT(o.id) AS orders_count, MIN(o.final_total) AS min_order_value, MAX(o.final_total) AS max_order_value, ROUND(AVG(o.final_total), 2) AS avg_order_value, SUM(o.final_total) AS total_sales_volume FROM restaurant_branches b JOIN orders o ON b.id = o.branch_id GROUP BY b.id, b.name ORDER BY total_sales_volume DESC;");

        register(37, "op_37_having",
                "37. HAVING", CAT_INTERMEDIATE,
                "orders GROUP BY branch_id HAVING COUNT >= 1",
                "Filters aggregated grouped output using post-aggregation HAVING predicates.",
                "SELECT b.name AS branch_name, COUNT(o.id) AS orders_count, SUM(o.final_total) AS total_revenue FROM restaurant_branches b JOIN orders o ON b.id = o.branch_id GROUP BY b.id, b.name HAVING COUNT(o.id) >= 1 ORDER BY total_revenue DESC;");

        register(38, "op_38_create_assertion",
                "38. CREATE ASSERTION", CAT_INTERMEDIATE,
                "Integrity Assertion (Capacity & Price Integrity)",
                "Standard SQL Assertion simulation verifying global table capacity and pricing sanity.",
                "SELECT 'ASSERTION: Tables capacity > 0 AND prices >= 0' AS assertion_rule, NOT EXISTS (SELECT 1 FROM dining_tables WHERE seating_capacity <= 0 OR deposit_amount < 0) AS passes_global_assertion;");

        register(39, "op_39_create_trigger",
                "39. CREATE TRIGGER", CAT_INTERMEDIATE,
                "TRIGGER trg_audit_order_changes",
                "Creates automated database trigger tracking transactional modifications.",
                "CREATE TABLE IF NOT EXISTS audit_trigger_log (log_id INT AUTO_INCREMENT PRIMARY KEY, event_type VARCHAR(50), fired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP); DROP TRIGGER IF EXISTS trg_audit_demo; CREATE TRIGGER trg_audit_demo AFTER INSERT ON dining_table_audit FOR EACH ROW INSERT INTO audit_trigger_log (event_type) VALUES ('INSERT_ON_AUDIT'); SELECT trigger_name, event_manipulation, event_object_table FROM information_schema.triggers WHERE trigger_schema = DATABASE() LIMIT 5;");

        register(40, "op_40_create_view",
                "40. CREATE VIEW", CAT_INTERMEDIATE,
                "VIEW v_active_bangalore_orders",
                "Creates reusable virtual relational view for active Bangalore restaurant orders.",
                "CREATE OR REPLACE VIEW v_active_bangalore_orders AS SELECT o.order_number, u.email AS customer_email, b.name AS branch_name, o.final_total, o.status FROM orders o JOIN users u ON o.user_id = u.id JOIN restaurant_branches b ON o.branch_id = b.id WHERE o.status NOT IN ('CANCELLED', 'REFUNDED'); SELECT * FROM v_active_bangalore_orders LIMIT 15;");

        register(41, "op_41_drop_view",
                "41. DROP VIEW", CAT_INTERMEDIATE,
                "VIEW v_temp_demo_view",
                "Safely drops temporary database view.",
                "CREATE OR REPLACE VIEW v_temp_demo_view AS SELECT 1 AS dummy; DROP VIEW IF EXISTS v_temp_demo_view; SELECT 'View v_temp_demo_view dropped successfully' AS operation_status;");

        register(42, "op_42_alter_table_schema_mod",
                "42. Schema Modification using ALTER TABLE", CAT_INTERMEDIATE,
                "dining_table_audit (Modifying Column Type)",
                "Modifies existing column specification and size dynamically via ALTER TABLE.",
                "CREATE TABLE IF NOT EXISTS dining_table_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, table_number VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, guest_capacity INT DEFAULT 4, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB; ALTER TABLE dining_table_audit MODIFY COLUMN status VARCHAR(60) NOT NULL; DESCRIBE dining_table_audit;");

        // =====================================================================
        // Advanced SQL Operations (43 - 76)
        // =====================================================================
        register(43, "op_43_union",
                "43. UNION", CAT_ADVANCED,
                "orders & tables (Distinct Reference IDs)",
                "Merges disjoint reference keys eliminating duplicate values across tables.",
                "SELECT order_number AS reference_code, 'FOOD_ORDER' AS transaction_type FROM orders UNION SELECT reference_no AS reference_code, 'TABLE_BOOKING' AS transaction_type FROM table_reservations LIMIT 20;");

        register(44, "op_44_union_all",
                "44. UNION ALL", CAT_ADVANCED,
                "table_reservations UNION ALL room_reservations UNION ALL hall_reservations",
                "Combines result sets across tables, private dining suites, and banquet reservations without duplicate suppression.",
                "SELECT 'TABLE RESERVATION' AS reservation_type, tr.reference_no, u.email AS customer, b.name AS branch, tr.total_price AS amount, tr.status FROM table_reservations tr JOIN users u ON tr.user_id = u.id JOIN restaurant_branches b ON tr.branch_id = b.id UNION ALL SELECT 'ROOM RESERVATION' AS reservation_type, rr.reference_no, u.email AS customer, b.name AS branch, rr.total_price AS amount, rr.status FROM room_reservations rr JOIN users u ON rr.user_id = u.id JOIN restaurant_branches b ON rr.branch_id = b.id UNION ALL SELECT 'HALL RESERVATION' AS reservation_type, hr.reference_no, u.email AS customer, b.name AS branch, hr.total_price AS amount, hr.status FROM hall_reservations hr JOIN users u ON hr.user_id = u.id JOIN restaurant_branches b ON hr.branch_id = b.id ORDER BY amount DESC LIMIT 20;");

        register(45, "op_45_intersect",
                "45. INTERSECT", CAT_ADVANCED,
                "users with Orders INTERSECT users with Table Bookings",
                "Identifies loyal cross-service patrons who have both placed food orders AND booked tables.",
                "SELECT DISTINCT u.id, u.email, CONCAT(u.first_name, ' ', u.last_name) AS customer_name FROM users u JOIN orders o ON u.id = o.user_id WHERE u.id IN (SELECT user_id FROM table_reservations) LIMIT 15;");

        register(46, "op_46_except",
                "46. EXCEPT", CAT_ADVANCED,
                "users with Orders EXCEPT users with Table Bookings",
                "Identifies digital-only delivery customers who have ordered food but never reserved a table.",
                "SELECT DISTINCT u.id, u.email, CONCAT(u.first_name, ' ', u.last_name) AS customer_name FROM users u JOIN orders o ON u.id = o.user_id WHERE u.id NOT IN (SELECT user_id FROM table_reservations WHERE user_id IS NOT NULL) LIMIT 15;");

        register(47, "op_47_case",
                "47. CASE", CAT_ADVANCED,
                "orders (Dynamic Tier Classification)",
                "Conditional business logic categorizing orders into billing and volume tiers.",
                "SELECT order_number, final_total, CASE WHEN final_total >= 1000 THEN 'PREMIUM_FEAST' WHEN final_total >= 600 THEN 'STANDARD_FAMILY' ELSE 'ECONOMY_SNACK' END AS order_tier, payment_method, status FROM orders ORDER BY final_total DESC LIMIT 15;");

        register(48, "op_48_with_cte",
                "48. WITH (Common Table Expression – CTE)", CAT_ADVANCED,
                "CTE branch_metrics",
                "Uses modular Common Table Expression (CTE) to compute branch sales benchmarks.",
                "WITH branch_metrics AS (SELECT b.id AS branch_id, b.name AS branch_name, COUNT(o.id) AS total_orders, SUM(o.final_total) AS total_revenue FROM restaurant_branches b JOIN orders o ON b.id = o.branch_id GROUP BY b.id, b.name) SELECT branch_name, total_orders, total_revenue, ROUND(total_revenue / total_orders, 2) AS avg_per_order FROM branch_metrics ORDER BY total_revenue DESC;");

        register(49, "op_49_join_on",
                "49. JOIN ON", CAT_ADVANCED,
                "orders JOIN order_items ON o.id = oi.order_id",
                "Explicit theta join on multi-table relational foreign key equality conditions.",
                "SELECT o.order_number, oi.item_name_snapshot AS dish_name, oi.unit_price_snapshot AS price, oi.quantity, oi.total_price FROM orders o JOIN order_items oi ON o.id = oi.order_id ORDER BY o.id DESC LIMIT 15;");

        register(50, "op_50_self_join",
                "50. SELF JOIN", CAT_ADVANCED,
                "users u1 JOIN users u2",
                "Correlates rows in the users table against itself based on common regional attributes.",
                "SELECT u1.id AS user_1_id, CONCAT(u1.first_name, ' ', u1.last_name) AS customer_one, u2.id AS user_2_id, CONCAT(u2.first_name, ' ', u2.last_name) AS customer_two, ua1.city AS common_city FROM users u1 JOIN users u2 ON u1.id < u2.id JOIN user_addresses ua1 ON u1.id = ua1.user_id JOIN user_addresses ua2 ON u2.id = ua2.user_id AND ua1.city = ua2.city LIMIT 15;");

        register(51, "op_51_natural_join",
                "51. NATURAL JOIN", CAT_ADVANCED,
                "orders NATURAL JOIN payments",
                "Relational join implicitly matching all identically named column attributes.",
                "SELECT p.id, p.payment_reference, p.amount, p.status AS payment_status FROM payments p NATURAL JOIN users u LIMIT 15;");

        register(52, "op_52_cross_join",
                "52. CROSS JOIN", CAT_ADVANCED,
                "restaurant_branches CROSS JOIN menu_categories",
                "Generates the complete Cartesian product matrix of branches and culinary categories.",
                "SELECT b.name AS branch_name, c.name AS dish_category FROM restaurant_branches b CROSS JOIN menu_categories c ORDER BY b.name, c.name LIMIT 20;");

        register(53, "op_53_inline_view",
                "53. INLINE VIEW", CAT_ADVANCED,
                "(SELECT ... FROM ...) AS subquery_view",
                "Executes query against dynamic inline derived table subquery view in FROM clause.",
                "SELECT iv.branch_name, iv.order_count, iv.gross_revenue FROM (SELECT b.name AS branch_name, COUNT(o.id) AS order_count, SUM(o.final_total) AS gross_revenue FROM restaurant_branches b JOIN orders o ON b.id = o.branch_id GROUP BY b.id, b.name) AS iv WHERE iv.order_count > 0 ORDER BY iv.gross_revenue DESC;");

        register(54, "op_54_commit",
                "54. COMMIT", CAT_ADVANCED,
                "Transaction Control (COMMIT)",
                "Demonstrates ACID transaction boundary completion making modifications permanent.",
                "START TRANSACTION; INSERT INTO dining_table_audit (table_number, status) VALUES ('T-COMMIT-TEST', 'CONFIRMED'); COMMIT; SELECT * FROM dining_table_audit WHERE table_number = 'T-COMMIT-TEST' LIMIT 1;");

        register(55, "op_55_rollback",
                "55. ROLLBACK", CAT_ADVANCED,
                "Transaction Control (ROLLBACK)",
                "Rolls back and discards uncommitted transactional modifications ensuring data integrity.",
                "START TRANSACTION; INSERT INTO dining_table_audit (table_number, status) VALUES ('T-ROLLBACK-TEST', 'DIRTY'); ROLLBACK; SELECT 'Transactional changes rolled back successfully' AS transaction_state;");

        register(56, "op_56_savepoint",
                "56. SAVEPOINT", CAT_ADVANCED,
                "Transaction Control (SAVEPOINT & ROLLBACK TO)",
                "Creates nested savepoint allowing partial rollback within active transaction scope.",
                "START TRANSACTION; INSERT INTO dining_table_audit (table_number, status) VALUES ('T-SP-1', 'STEP1'); SAVEPOINT my_savepoint; INSERT INTO dining_table_audit (table_number, status) VALUES ('T-SP-2', 'STEP2'); ROLLBACK TO SAVEPOINT my_savepoint; COMMIT; SELECT 'Rolled back to savepoint successfully' AS outcome;");

        register(57, "op_57_grant",
                "57. GRANT", CAT_ADVANCED,
                "DCL: Access Privilege Grant",
                "Inspects user database privileges and demonstrated GRANT permissions.",
                "SHOW GRANTS FOR CURRENT_USER();");

        register(58, "op_58_revoke",
                "58. REVOKE", CAT_ADVANCED,
                "DCL: Privilege Inspection",
                "Inspects user privileges and authorities recorded in the database.",
                "SELECT user, host FROM mysql.user WHERE user = SUBSTRING_INDEX(USER(), '@', 1);");

        register(59, "op_59_on_delete_cascade",
                "59. ON DELETE CASCADE", CAT_ADVANCED,
                "Foreign Key Rule: ON DELETE CASCADE",
                "Inspects foreign keys configured with ON DELETE CASCADE propagation.",
                "SELECT rc.constraint_name, rc.table_name, rc.referenced_table_name, rc.delete_rule FROM information_schema.referential_constraints rc WHERE rc.constraint_schema = DATABASE() AND rc.delete_rule = 'CASCADE' LIMIT 15;");

        register(60, "op_60_on_update_cascade",
                "60. ON UPDATE CASCADE", CAT_ADVANCED,
                "Foreign Key Rule: ON UPDATE CASCADE",
                "Inspects foreign keys configured with ON UPDATE CASCADE propagation.",
                "SELECT rc.constraint_name, rc.table_name, rc.referenced_table_name, rc.update_rule FROM information_schema.referential_constraints rc WHERE rc.constraint_schema = DATABASE() LIMIT 15;");

        register(61, "op_61_drop_cascade",
                "61. DROP CASCADE", CAT_ADVANCED,
                "Constraint Removal with Cascade Propagation",
                "Demonstrates dependent cascade removal semantics on database entities.",
                "CREATE TABLE IF NOT EXISTS temp_cascade_child (id INT PRIMARY KEY, val VARCHAR(50)); DROP TABLE IF EXISTS temp_cascade_child; SELECT 'Child entity dropped with cascade safety' AS cascade_status;");

        register(62, "op_62_drop_restrict",
                "62. DROP RESTRICT", CAT_ADVANCED,
                "Referential Integrity Restrict Verification",
                "Verifies RESTRICT constraint rules preventing drop when dependent foreign keys exist.",
                "SELECT rc.constraint_name, rc.table_name, rc.referenced_table_name, rc.delete_rule FROM information_schema.referential_constraints rc WHERE rc.constraint_schema = DATABASE() AND rc.delete_rule IN ('RESTRICT', 'NO ACTION') LIMIT 15;");

        register(63, "op_63_alter_add_column",
                "63. ALTER TABLE ADD COLUMN", CAT_ADVANCED,
                "ALTER TABLE ADD COLUMN",
                "Appends dynamic attribute column to table schema definition.",
                "CREATE TABLE IF NOT EXISTS dining_table_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, table_number VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, guest_capacity INT DEFAULT 4, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB; ALTER TABLE dining_table_audit ADD COLUMN IF NOT EXISTS tracking_tag VARCHAR(100) DEFAULT 'STANDARD'; DESCRIBE dining_table_audit;");

        register(64, "op_64_alter_drop_column",
                "64. ALTER TABLE DROP COLUMN", CAT_ADVANCED,
                "ALTER TABLE DROP COLUMN",
                "Removes column attribute cleanly from table schema definition.",
                "CREATE TABLE IF NOT EXISTS dining_table_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, table_number VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, guest_capacity INT DEFAULT 4, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB; ALTER TABLE dining_table_audit ADD COLUMN IF NOT EXISTS dummy_col VARCHAR(10); ALTER TABLE dining_table_audit DROP COLUMN IF EXISTS dummy_col; DESCRIBE dining_table_audit;");

        register(65, "op_65_alter_add_constraint",
                "65. ALTER TABLE ADD CONSTRAINT", CAT_ADVANCED,
                "ALTER TABLE ADD CONSTRAINT",
                "Appends custom named UNIQUE constraint to relational table.",
                "CREATE TABLE IF NOT EXISTS dining_table_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, table_number VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, guest_capacity INT DEFAULT 4, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB; ALTER TABLE dining_table_audit ADD CONSTRAINT unique_audit_token UNIQUE (audit_id); SELECT constraint_name, table_name FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'dining_table_audit';");

        register(66, "op_66_alter_drop_constraint",
                "66. ALTER TABLE DROP CONSTRAINT", CAT_ADVANCED,
                "ALTER TABLE DROP CONSTRAINT / INDEX",
                "Drops specified constraint from table schema definition.",
                "CREATE TABLE IF NOT EXISTS dining_table_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, table_number VARCHAR(20) NOT NULL, status VARCHAR(30) NOT NULL, guest_capacity INT DEFAULT 4, recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB; ALTER TABLE dining_table_audit DROP INDEX IF EXISTS unique_audit_token; SELECT 'Constraint dropped successfully' AS outcome;");

        register(67, "op_67_create_index",
                "67. CREATE INDEX", CAT_ADVANCED,
                "CREATE INDEX idx_orders_type_status",
                "Creates secondary B-Tree composite index accelerating queries on orders.",
                "CREATE INDEX IF NOT EXISTS idx_orders_type_status ON orders (order_type, status); SHOW INDEX FROM orders WHERE Key_name = 'idx_orders_type_status';");

        register(68, "op_68_drop_index",
                "68. DROP INDEX", CAT_ADVANCED,
                "DROP INDEX idx_temp_demo",
                "Drops secondary B-Tree index from table.",
                "CREATE INDEX IF NOT EXISTS idx_temp_demo ON orders (created_at); DROP INDEX IF EXISTS idx_temp_demo ON orders; SELECT 'Index idx_temp_demo dropped successfully' AS outcome;");

        register(69, "op_69_distinct",
                "69. DISTINCT", CAT_ADVANCED,
                "orders (DISTINCT payment methods & types)",
                "Suppresses duplicate values returning only unique attribute combinations.",
                "SELECT DISTINCT order_type, payment_method, status FROM orders ORDER BY order_type, payment_method;");

        register(70, "op_70_limit",
                "70. LIMIT", CAT_ADVANCED,
                "orders LIMIT 10",
                "Restricts cardinality of tuples returned from query execution to top 10.",
                "SELECT order_number, order_type, status, final_total FROM orders ORDER BY id DESC LIMIT 10;");

        register(71, "op_71_offset",
                "71. OFFSET", CAT_ADVANCED,
                "orders LIMIT 10 OFFSET 5",
                "Performs pagination skipping first 5 rows and fetching subsequent 10 rows.",
                "SELECT order_number, order_type, status, final_total, created_at FROM orders ORDER BY id DESC LIMIT 10 OFFSET 5;");

        register(72, "op_72_row_number",
                "72. ROW_NUMBER()", CAT_ADVANCED,
                "ROW_NUMBER() OVER(PARTITION BY branch_id ORDER BY final_total DESC)",
                "Window function assigning sequential integer sequence to rows partitioned by branch.",
                "SELECT ROW_NUMBER() OVER (PARTITION BY b.name ORDER BY o.final_total DESC) AS rank_in_branch, b.name AS branch_name, o.order_number, o.final_total, o.status FROM orders o JOIN restaurant_branches b ON o.branch_id = b.id LIMIT 20;");

        register(73, "op_73_rank",
                "73. RANK()", CAT_ADVANCED,
                "RANK() OVER(ORDER BY final_total DESC)",
                "Window function computing rank with gaps for ties in sales value.",
                "SELECT RANK() OVER (ORDER BY final_total DESC) AS global_sales_rank, order_number, final_total, payment_method, status FROM orders LIMIT 20;");

        register(74, "op_74_dense_rank",
                "74. DENSE_RANK()", CAT_ADVANCED,
                "DENSE_RANK() OVER(ORDER BY base_price DESC)",
                "Window function computing rank without gaps for ties in dish prices.",
                "SELECT DENSE_RANK() OVER (ORDER BY base_price DESC) AS price_dense_rank, name AS dish_name, base_price, is_vegetarian FROM menu_items LIMIT 20;");

        register(75, "op_75_partition_by",
                "75. PARTITION BY", CAT_ADVANCED,
                "OVER (PARTITION BY order_type)",
                "Divides query result set into partitions to apply aggregate calculations per partition.",
                "SELECT order_number, order_type, final_total, ROUND(AVG(final_total) OVER (PARTITION BY order_type), 2) AS avg_for_order_type, COUNT(id) OVER (PARTITION BY order_type) AS total_in_order_type FROM orders LIMIT 20;");

        register(76, "op_76_over",
                "76. OVER()", CAT_ADVANCED,
                "OVER (ORDER BY created_at)",
                "Defines window specification for cumulative running totals across transactions.",
                "SELECT order_number, final_total, SUM(final_total) OVER (ORDER BY id ASC) AS cumulative_running_revenue_inr, created_at FROM orders LIMIT 20;");
    }

    public static List<SqlOperationDef> getAllOperations() {
        return Collections.unmodifiableList(ALL_OPERATIONS);
    }

    public static SqlOperationDef getByKey(String key) {
        return BY_KEY.get(key);
    }

    public static Map<String, List<SqlOperationDef>> getCategorizedOperations() {
        Map<String, List<SqlOperationDef>> map = new LinkedHashMap<>();
        for (SqlOperationDef def : ALL_OPERATIONS) {
            map.computeIfAbsent(def.getCategory(), k -> new ArrayList<>()).add(def);
        }
        return map;
    }
}
