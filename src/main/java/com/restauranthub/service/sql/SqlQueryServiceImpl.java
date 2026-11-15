package com.restauranthub.service.sql;

import com.restauranthub.dto.sql.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SqlQueryServiceImpl implements SqlQueryService {

    private static final Logger log = LoggerFactory.getLogger(SqlQueryServiceImpl.class);

    private final DataSource dataSource;

    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "^(?i)\\s*(SELECT|SHOW|DESCRIBE|DESC|EXPLAIN|WITH)\\b.*", Pattern.DOTALL);
    private static final Pattern DDL_PATTERN = Pattern.compile(
            "^(?i)\\s*(CREATE|DROP|ALTER|TRUNCATE|RENAME)\\b.*", Pattern.DOTALL);
    private static final Pattern DML_PATTERN = Pattern.compile(
            "^(?i)\\s*(INSERT|UPDATE|DELETE|REPLACE|CALL)\\b.*", Pattern.DOTALL);

    @Override
    public SqlBatchResultDto executeQuery(String sql, Integer maxRows) {
        return executeQuery(sql, maxRows, false);
    }

    @Override
    public SqlBatchResultDto executeQuery(String sql, Integer maxRows, boolean stopOnError) {
        if (sql == null || sql.trim().isEmpty()) {
            return SqlBatchResultDto.builder()
                    .results(Collections.emptyList())
                    .totalQueries(0)
                    .successfulQueries(0)
                    .failedQueries(0)
                    .totalExecutionTimeMs(0)
                    .build();
        }

        int effectiveMaxRows = (maxRows != null && maxRows > 0) ? Math.min(maxRows, 5000) : 500;
        List<String> statements = splitSqlStatements(sql);
        List<SqlQueryResultDto> results = new ArrayList<>();

        long batchStart = System.currentTimeMillis();
        int successCount = 0;
        int failCount = 0;

        try (Connection conn = dataSource.getConnection()) {
            for (String stmt : statements) {
                String cleanStmt = stmt.trim();
                if (cleanStmt.isEmpty()) {
                    continue;
                }

                SqlQueryResultDto res = executeSingleStatement(conn, cleanStmt, effectiveMaxRows);
                results.add(res);

                if (res.isSuccess()) {
                    successCount++;
                } else {
                    failCount++;
                    if (stopOnError) {
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Failed to acquire DB connection for SQL execution: {}", e.getMessage());
            results.add(SqlQueryResultDto.builder()
                    .query(sql)
                    .success(false)
                    .errorMessage("Database connection failure: " + e.getMessage())
                    .sqlState(e.getSQLState())
                    .errorCode(e.getErrorCode())
                    .build());
            failCount++;
        }

        long batchDuration = System.currentTimeMillis() - batchStart;

        return SqlBatchResultDto.builder()
                .results(results)
                .totalQueries(results.size())
                .successfulQueries(successCount)
                .failedQueries(failCount)
                .totalExecutionTimeMs(batchDuration)
                .build();
    }

    private SqlQueryResultDto executeSingleStatement(Connection conn, String sql, int maxRows) {
        long startNano = System.nanoTime();
        String queryType = determineQueryType(sql);

        try (Statement statement = conn.createStatement()) {
            statement.setMaxRows(maxRows);
            statement.setQueryTimeout(60); // 60s timeout

            boolean hasResultSet = statement.execute(sql);
            long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;

            if (hasResultSet) {
                try (ResultSet rs = statement.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    List<String> columnNames = new ArrayList<>(colCount);
                    List<String> columnTypes = new ArrayList<>(colCount);

                    for (int i = 1; i <= colCount; i++) {
                        String label = meta.getColumnLabel(i);
                        columnNames.add(label != null && !label.isEmpty() ? label : meta.getColumnName(i));
                        columnTypes.add(meta.getColumnTypeName(i));
                    }

                    List<Map<String, Object>> rows = new ArrayList<>();
                    int rowCount = 0;

                    while (rs.next() && rowCount < maxRows) {
                        Map<String, Object> row = new LinkedHashMap<>(colCount);
                        for (int i = 1; i <= colCount; i++) {
                            String colName = columnNames.get(i - 1);
                            Object val = rs.getObject(i);
                            row.put(colName, formatValue(val));
                        }
                        rows.add(row);
                        rowCount++;
                    }

                    return SqlQueryResultDto.builder()
                            .query(sql)
                            .queryType("SELECT")
                            .columns(columnNames)
                            .columnTypes(columnTypes)
                            .rows(rows)
                            .rowCount(rowCount)
                            .affectedRows(0)
                            .executionTimeMs(elapsedMs)
                            .success(true)
                            .build();
                }
            } else {
                int updateCount = statement.getUpdateCount();
                return SqlQueryResultDto.builder()
                        .query(sql)
                        .queryType(queryType)
                        .columns(Collections.emptyList())
                        .columnTypes(Collections.emptyList())
                        .rows(Collections.emptyList())
                        .rowCount(0)
                        .affectedRows(Math.max(updateCount, 0))
                        .executionTimeMs(elapsedMs)
                        .success(true)
                        .build();
            }

        } catch (SQLException e) {
            long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;
            log.warn("SQL execution error [{}]: {}", e.getSQLState(), e.getMessage());

            return SqlQueryResultDto.builder()
                    .query(sql)
                    .queryType(queryType)
                    .columns(Collections.emptyList())
                    .columnTypes(Collections.emptyList())
                    .rows(Collections.emptyList())
                    .rowCount(0)
                    .affectedRows(0)
                    .executionTimeMs(elapsedMs)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .sqlState(e.getSQLState())
                    .errorCode(e.getErrorCode())
                    .build();
        }
    }

    private String determineQueryType(String sql) {
        String trimmed = sql.trim();
        if (SELECT_PATTERN.matcher(trimmed).matches()) return "SELECT";
        if (DDL_PATTERN.matcher(trimmed).matches()) return "DDL";
        if (DML_PATTERN.matcher(trimmed).matches()) return "DML";
        return "OTHER";
    }

    private Object formatValue(Object val) {
        if (val == null) return null;
        if (val instanceof byte[] bytes) {
            return "[BLOB " + bytes.length + " bytes]";
        }
        if (val instanceof java.util.Date d) {
            return d.toString();
        }
        return val.toString();
    }

    @Override
    public List<String> getAllTableNames() {
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String catalog = conn.getCatalog();
            try (ResultSet rs = metaData.getTables(catalog, null, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to list tables: {}", e.getMessage());
        }
        Collections.sort(tables);
        return tables;
    }

    @Override
    public List<TableSchemaDto> getDatabaseSchemaOverview() {
        List<TableSchemaDto> list = new ArrayList<>();
        List<String> tableNames = getAllTableNames();

        try (Connection conn = dataSource.getConnection()) {
            for (String tableName : tableNames) {
                list.add(extractTableSchema(conn, tableName));
            }
        } catch (SQLException e) {
            log.error("Failed to extract full schema: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public TableSchemaDto getTableDetails(String tableName) {
        try (Connection conn = dataSource.getConnection()) {
            return extractTableSchema(conn, tableName);
        } catch (SQLException e) {
            log.error("Failed to get table details for {}: {}", tableName, e.getMessage());
            return TableSchemaDto.builder()
                    .tableName(tableName)
                    .rowCount(0)
                    .columns(Collections.emptyList())
                    .build();
        }
    }

    private TableSchemaDto extractTableSchema(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        String catalog = conn.getCatalog();

        // Get Primary Keys
        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet pkRs = meta.getPrimaryKeys(catalog, null, tableName)) {
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
        } catch (Exception ignored) {
        }

        // Get Columns
        List<TableSchemaDto.ColumnDetailDto> columns = new ArrayList<>();
        try (ResultSet colRs = meta.getColumns(catalog, null, tableName, "%")) {
            while (colRs.next()) {
                String colName = colRs.getString("COLUMN_NAME");
                String typeName = colRs.getString("TYPE_NAME");
                int colSize = colRs.getInt("COLUMN_SIZE");
                int nullable = colRs.getInt("NULLABLE");
                String defVal = colRs.getString("COLUMN_DEF");

                String fullType = typeName;
                if (colSize > 0 && !typeName.equalsIgnoreCase("TEXT") &&
                    !typeName.equalsIgnoreCase("TIMESTAMP") && !typeName.equalsIgnoreCase("DATETIME") &&
                    !typeName.equalsIgnoreCase("INT") && !typeName.equalsIgnoreCase("BIGINT")) {
                    fullType += "(" + colSize + ")";
                }

                columns.add(TableSchemaDto.ColumnDetailDto.builder()
                        .name(colName)
                        .type(fullType)
                        .nullable(nullable == DatabaseMetaData.columnNullable)
                        .primaryKey(primaryKeys.contains(colName))
                        .defaultValue(defVal)
                        .build());
            }
        }

        // Row count
        long rowCount = 0;
        try (Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM `" + tableName + "`")) {
                if (rs.next()) {
                    rowCount = rs.getLong(1);
                }
            }
        } catch (Exception ignored) {
        }

        return TableSchemaDto.builder()
                .tableName(tableName)
                .rowCount(rowCount)
                .columns(columns)
                .build();
    }

    /**
     * Splits a multi-statement SQL script by semicolons, properly respecting quotes, comments, and strings.
     */
    public static List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        char[] chars = script.toCharArray();
        int len = chars.length;

        for (int i = 0; i < len; i++) {
            char c = chars[i];
            char next = (i + 1 < len) ? chars[i + 1] : '\0';

            // Check line comment
            if (inLineComment) {
                current.append(c);
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                }
                continue;
            }

            // Check block comment
            if (inBlockComment) {
                current.append(c);
                if (c == '*' && next == '/') {
                    current.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            // Start comments if not inside string
            if (!inSingleQuote && !inDoubleQuote && !inBacktick) {
                if (c == '-' && next == '-') {
                    current.append(c).append(next);
                    i++;
                    inLineComment = true;
                    continue;
                }
                if (c == '/' && next == '*') {
                    current.append(c).append(next);
                    i++;
                    inBlockComment = true;
                    continue;
                }
            }

            // Handle string and identifier boundaries
            if (c == '\'' && !inDoubleQuote && !inBacktick) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            }

            // Semicolon separator
            if (c == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick) {
                String trimmed = current.toString().trim();
                if (!trimmed.isEmpty()) {
                    statements.add(trimmed);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }

        return statements;
    }

    @Override
    public List<PredefinedQueryDto> getAllPredefinedQueries() {
        List<PredefinedQueryDto> list = new ArrayList<>();
        Map<String, List<PredefinedQueryDto>> map = getCategorizedPredefinedQueries();
        for (List<PredefinedQueryDto> sub : map.values()) {
            list.addAll(sub);
        }
        return list;
    }

    @Override
    public SqlBatchResultDto executeAllPredefinedQueries() {
        StringBuilder sb = new StringBuilder();
        for (PredefinedQueryDto q : getAllPredefinedQueries()) {
            sb.append("-- ==========================================\n");
            sb.append("-- Module: ").append(q.getModule()).append(" | Query: ").append(q.getName()).append("\n");
            sb.append("-- ==========================================\n");
            sb.append(q.getSql().trim());
            if (!q.getSql().trim().endsWith(";")) {
                sb.append(";");
            }
            sb.append("\n\n");
        }
        return executeQuery(sb.toString(), 200, false);
    }

    @Override
    public Map<String, List<PredefinedQueryDto>> getCategorizedPredefinedQueries() {
        Map<String, List<PredefinedQueryDto>> map = new LinkedHashMap<>();

        // 1. User Authentication & Authorization
        map.put("1. User Auth & Roles", List.of(
                PredefinedQueryDto.builder()
                        .id("auth-all-users")
                        .module("User Auth & Roles")
                        .name("List All Users with Roles & Status")
                        .description("Retrieves full user roster with mapped role names and contact phone")
                        .sql("SELECT u.id, u.email, CONCAT(u.first_name, ' ', u.last_name) AS full_name, u.phone_number, u.status, u.email_verified, r.name AS role, u.created_at FROM users u LEFT JOIN user_roles ur ON u.id = ur.user_id LEFT JOIN roles r ON ur.role_id = r.id ORDER BY u.id ASC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("auth-role-summary")
                        .module("User Auth & Roles")
                        .name("User Count by Role")
                        .description("Aggregates total user count per assigned system authority")
                        .sql("SELECT r.name AS role_name, r.description, COUNT(ur.user_id) AS total_assigned_users FROM roles r LEFT JOIN user_roles ur ON r.id = ur.role_id GROUP BY r.id, r.name, r.description ORDER BY total_assigned_users DESC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("auth-addresses")
                        .module("User Auth & Roles")
                        .name("Customer Saved Delivery Addresses")
                        .description("Lists customer delivery address book with locality and landmark")
                        .sql("SELECT ua.id, u.email, ua.address_type, ua.street_address, ua.city, ua.postal_code, ua.is_default FROM user_addresses ua JOIN users u ON ua.user_id = u.id ORDER BY ua.id DESC;")
                        .queryType("SELECT")
                        .build()
        ));

        // 2. Restaurant Branches
        map.put("2. Restaurant Branches", List.of(
                PredefinedQueryDto.builder()
                        .id("branch-roster")
                        .module("Restaurant Branches")
                        .name("Bangalore Branches Overview")
                        .description("Details of all 5 operational branches across Bangalore prime hubs")
                        .sql("SELECT id, name, code, address, city, postal_code, phone_number, email, is_active FROM restaurant_branches ORDER BY id ASC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("branch-hours")
                        .module("Restaurant Branches")
                        .name("Branch Operating Hours Schedule")
                        .description("Weekly schedule of opening, closing, and break times per branch")
                        .sql("SELECT b.name AS branch_name, oh.day_of_week, oh.open_time, oh.close_time, oh.is_closed FROM operating_hours oh JOIN restaurant_branches b ON oh.branch_id = b.id ORDER BY b.id, FIELD(oh.day_of_week, 'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY');")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("branch-staff")
                        .module("Restaurant Branches")
                        .name("Staff & Manager Branch Assignments")
                        .description("Maps staff members, general managers and executive chefs to branches")
                        .sql("SELECT sba.id, b.name AS branch_name, CONCAT(u.first_name, ' ', u.last_name) AS staff_name, u.email, sba.designation FROM staff_branch_assignments sba JOIN users u ON sba.user_id = u.id JOIN restaurant_branches b ON sba.branch_id = b.id ORDER BY b.id;")
                        .queryType("SELECT")
                        .build()
        ));

        // 3. Dining Table Reservations
        map.put("3. Dining Tables", List.of(
                PredefinedQueryDto.builder()
                        .id("tables-inventory")
                        .module("Dining Tables")
                        .name("All Dining Tables & Seating Capacities")
                        .description("Table inventory by section (Rooftop, Patio, Main Dining) and capacity")
                        .sql("SELECT dt.id, b.name AS branch, dt.table_number, dt.seating_capacity, dt.table_type, dt.section, dt.deposit_amount, dt.is_active FROM dining_tables dt JOIN restaurant_branches b ON dt.branch_id = b.id ORDER BY b.name, dt.table_number;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("tables-reservations")
                        .module("Dining Tables")
                        .name("Confirmed & Active Table Reservations")
                        .description("Live table booking bookings with guest counts, dates and status")
                        .sql("SELECT tr.id, tr.reference_no, u.email AS customer, b.name AS branch, dt.table_number, tr.reservation_date, tr.start_time, tr.guest_count, tr.status, tr.deposit_amount, tr.created_at FROM table_reservations tr JOIN users u ON tr.user_id = u.id JOIN dining_tables dt ON tr.table_id = dt.id JOIN restaurant_branches b ON tr.branch_id = b.id ORDER BY tr.created_at DESC;")
                        .queryType("SELECT")
                        .build()
        ));

        // 4. Private Suites & Rooms
        map.put("4. Private Rooms", List.of(
                PredefinedQueryDto.builder()
                        .id("rooms-inventory")
                        .module("Private Rooms")
                        .name("VIP Private Dining Suites")
                        .description("List of luxury dining suites with hourly rate and capacities")
                        .sql("SELECT dr.id, b.name AS branch, dr.room_name, dr.min_capacity, dr.max_capacity, dr.hourly_rate, dr.deposit_amount, dr.amenities, dr.is_active FROM dining_rooms dr JOIN restaurant_branches b ON dr.branch_id = b.id;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("rooms-bookings")
                        .module("Private Rooms")
                        .name("Private Room Booking Records")
                        .description("Customer reservations for VIP suites with durations and status")
                        .sql("SELECT rr.id, rr.reference_no, u.email AS customer, dr.room_name, rr.reservation_date, rr.start_time, rr.duration_hours, rr.guest_count, rr.status, rr.total_price FROM room_reservations rr JOIN users u ON rr.user_id = u.id JOIN dining_rooms dr ON rr.room_id = dr.id ORDER BY rr.created_at DESC;")
                        .queryType("SELECT")
                        .build()
        ));

        // 5. Party Halls & Banquets
        map.put("5. Party Halls", List.of(
                PredefinedQueryDto.builder()
                        .id("halls-inventory")
                        .module("Party Halls")
                        .name("Party Halls & Banquet Spaces")
                        .description("Available banquet halls with maximum event capacity and base rate")
                        .sql("SELECT ph.id, b.name AS branch, ph.hall_name, ph.capacity, ph.base_rate, ph.deposit_amount, ph.is_active FROM party_halls ph JOIN restaurant_branches b ON ph.branch_id = b.id;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("halls-packages")
                        .module("Party Halls")
                        .name("Catering Packages for Party Halls")
                        .description("Package pricing tiers (Silver, Gold, Platinum) with plate inclusions")
                        .sql("SELECT hp.id, ph.hall_name, hp.package_name, hp.price_per_guest, hp.description, hp.is_active FROM hall_packages hp JOIN party_halls ph ON hp.hall_id = ph.id ORDER BY ph.id, hp.price_per_guest;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("halls-reservations")
                        .module("Party Halls")
                        .name("Party Hall Event Bookings")
                        .description("Scheduled events, weddings, corporate dinners and banquet bookings")
                        .sql("SELECT hr.id, hr.reference_no, u.email AS customer, ph.hall_name, hp.package_name, hr.reservation_date AS event_date, hr.event_type, hr.guest_count, hr.status, hr.total_price FROM hall_reservations hr JOIN users u ON hr.user_id = u.id JOIN party_halls ph ON hr.hall_id = ph.id LEFT JOIN hall_packages hp ON hr.package_id = hp.id ORDER BY hr.created_at DESC;")
                        .queryType("SELECT")
                        .build()
        ));

        // 6. Menu Items & Categories
        map.put("6. Menu & Categories", List.of(
                PredefinedQueryDto.builder()
                        .id("menu-catalog")
                        .module("Menu & Categories")
                        .name("Full Menu Catalog with Categories & Prices")
                        .description("Complete menu listing categorized across all branches with dietary tags")
                        .sql("SELECT m.id, b.name AS branch, c.name AS category, m.name AS item_name, m.base_price, m.is_vegetarian, m.is_vegan, m.is_available, m.tax_rate_percent FROM menu_items m JOIN restaurant_branches b ON m.branch_id = b.id JOIN menu_categories c ON m.category_id = c.id ORDER BY b.name, c.display_order, m.name;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("menu-category-counts")
                        .module("Menu & Categories")
                        .name("Category Breakdown & Item Counts")
                        .description("Summary of dish counts per culinary category")
                        .sql("SELECT c.id, c.name AS category, COUNT(m.id) AS total_items, MIN(m.base_price) AS min_price, MAX(m.base_price) AS max_price, AVG(m.base_price) AS avg_price FROM menu_categories c LEFT JOIN menu_items m ON c.id = m.category_id GROUP BY c.id, c.name ORDER BY c.display_order;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("menu-addons")
                        .module("Menu & Categories")
                        .name("Menu Item Addons & Variants")
                        .description("Customizable extra toppings, sizes, and addons linked to dishes")
                        .sql("SELECT a.id, m.name AS dish, a.name AS addon_name, a.price AS addon_price FROM menu_item_addons a JOIN menu_items m ON a.menu_item_id = m.id ORDER BY m.name, a.price;")
                        .queryType("SELECT")
                        .build()
        ));

        // 7. Inventory Management
        map.put("7. Inventory", List.of(
                PredefinedQueryDto.builder()
                        .id("inv-stock-level")
                        .module("Inventory")
                        .name("Current Inventory Stock & Low Stock Warnings")
                        .description("Inventory on hand with threshold warnings for dishes and raw ingredients")
                        .sql("SELECT inv.id, m.name AS item_name, inv.quantity_in_stock, inv.min_threshold, inv.unit_of_measure, (inv.quantity_in_stock <= inv.min_threshold) AS is_critically_low FROM inventory_items inv JOIN menu_items m ON inv.menu_item_id = m.id ORDER BY is_critically_low DESC, inv.quantity_in_stock ASC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("inv-movements")
                        .module("Inventory")
                        .name("Recent Inventory Stock Movements")
                        .description("Audit trail of order sales deductions and restocking adjustments")
                        .sql("SELECT mov.id, m.name AS item_name, mov.quantity_delta, mov.movement_type, mov.reference_note, mov.created_at FROM inventory_movements mov JOIN inventory_items inv ON mov.inventory_item_id = inv.id JOIN menu_items m ON inv.menu_item_id = m.id ORDER BY mov.created_at DESC LIMIT 30;")
                        .queryType("SELECT")
                        .build()
        ));

        // 8. Food Carts
        map.put("8. Food Carts", List.of(
                PredefinedQueryDto.builder()
                        .id("cart-summary")
                        .module("Food Carts")
                        .name("Active Customer Shopping Carts")
                        .description("Active cart items in session before order placement")
                        .sql("SELECT c.id AS cart_id, u.email AS customer, b.name AS branch, ci.id AS cart_item_id, m.name AS dish, ci.quantity, m.base_price, (ci.quantity * m.base_price) AS line_total, c.updated_at FROM carts c JOIN users u ON c.user_id = u.id LEFT JOIN restaurant_branches b ON c.branch_id = b.id JOIN cart_items ci ON c.id = ci.cart_id JOIN menu_items m ON ci.menu_item_id = m.id ORDER BY c.updated_at DESC;")
                        .queryType("SELECT")
                        .build()
        ));

        // 9. Food Orders & Order Items
        map.put("9. Food Orders", List.of(
                PredefinedQueryDto.builder()
                        .id("orders-recent")
                        .module("Food Orders")
                        .name("Recent Food Orders (Full Detail)")
                        .description("Master list of orders placed with customer, branch, totals and status")
                        .sql("SELECT o.id, o.order_number, u.email AS customer, b.name AS branch, o.order_type, o.status, o.subtotal, o.tax_amount, o.discount_amount, o.delivery_fee, o.final_total, o.payment_method, o.created_at FROM orders o JOIN users u ON o.user_id = u.id JOIN restaurant_branches b ON o.branch_id = b.id ORDER BY o.created_at DESC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("orders-items-breakdown")
                        .module("Food Orders")
                        .name("Order Items Breakdown")
                        .description("Specific dishes, unit prices, and quantities per ordered transaction")
                        .sql("SELECT oi.id, o.order_number, oi.item_name_snapshot AS item_name, oi.unit_price_snapshot AS unit_price, oi.quantity, oi.total_price, oi.special_instructions FROM order_items oi JOIN orders o ON oi.order_id = o.id ORDER BY o.id DESC, oi.id ASC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("orders-status-history")
                        .module("Food Orders")
                        .name("Order Lifecycle Status History")
                        .description("Detailed audit log of status progressions from PLACED to DELIVERED")
                        .sql("SELECT osh.id, o.order_number, osh.from_status, osh.to_status, osh.comment, osh.changed_at AS created_at FROM order_status_history osh JOIN orders o ON osh.order_id = o.id ORDER BY osh.changed_at DESC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("orders-status-counts")
                        .module("Food Orders")
                        .name("Orders Aggregation by Status")
                        .description("Order volumes and total gross value grouped by status")
                        .sql("SELECT status, COUNT(*) AS total_orders, SUM(final_total) AS total_value, AVG(final_total) AS avg_order_value FROM orders GROUP BY status ORDER BY total_orders DESC;")
                        .queryType("SELECT")
                        .build()
        ));

        // 10. Delivery Tracking
        map.put("10. Delivery Tracking", List.of(
                PredefinedQueryDto.builder()
                        .id("tracking-live")
                        .module("Delivery Tracking")
                        .name("Live Delivery Tracking & Driver Status")
                        .description("Real-time delivery partner assignment, vehicle number, and tracking state")
                        .sql("SELECT dt.id, o.order_number, dt.status, dt.driver_name, dt.driver_phone, dt.vehicle_number, dt.current_location, dt.estimated_delivery_minutes, dt.status_updated_at FROM delivery_tracking dt JOIN orders o ON dt.order_id = o.id ORDER BY dt.status_updated_at DESC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("tracking-active-deliveries")
                        .module("Delivery Tracking")
                        .name("Active In-Flight Deliveries Only")
                        .description("Filters non-terminal delivery orders currently progressing to destination")
                        .sql("SELECT dt.id, o.order_number, u.email AS customer, o.delivery_address, dt.status, dt.driver_name, dt.vehicle_number, dt.status_updated_at FROM delivery_tracking dt JOIN orders o ON dt.order_id = o.id JOIN users u ON o.user_id = u.id WHERE dt.status NOT IN ('DELIVERED', 'COMPLETED', 'CANCELLED', 'REFUNDED') ORDER BY dt.status_updated_at DESC;")
                        .queryType("SELECT")
                        .build()
        ));

        // 11. Payments & Refunds
        map.put("11. Payments & Refunds", List.of(
                PredefinedQueryDto.builder()
                        .id("pay-transactions")
                        .module("Payments & Refunds")
                        .name("Payment Transactions & Gateway Logs")
                        .description("Ledger of transactions with provider (MOCK, STRIPE, RAZORPAY), status and amounts")
                        .sql("SELECT p.id, p.payment_reference, o.order_number, u.email AS customer, p.amount, p.currency, p.provider, p.payment_method, p.status, p.gateway_transaction_id, p.created_at FROM payments p JOIN users u ON p.user_id = u.id LEFT JOIN orders o ON p.order_id = o.id ORDER BY p.created_at DESC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("pay-by-provider")
                        .module("Payments & Refunds")
                        .name("Revenue Volume by Payment Method & Provider")
                        .description("Financial rollup of processed revenues per payment gateway")
                        .sql("SELECT provider, payment_method, status, COUNT(*) AS transaction_count, SUM(amount) AS total_volume_inr FROM payments GROUP BY provider, payment_method, status ORDER BY total_volume_inr DESC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("pay-refunds")
                        .module("Payments & Refunds")
                        .name("Processed Customer Refunds")
                        .description("Refund records for cancelled orders or deposit returns")
                        .sql("SELECT r.id, p.payment_reference, r.refund_reference, r.amount AS refund_amount, r.reason, r.status, r.created_at FROM refunds r JOIN payments p ON r.payment_id = p.id ORDER BY r.created_at DESC;")
                        .queryType("SELECT")
                        .build()
        ));

        // 12. Parking Reservations
        map.put("12. Parking", List.of(
                PredefinedQueryDto.builder()
                        .id("park-slots")
                        .module("Parking")
                        .name("Parking Slots Inventory by Branch")
                        .description("Vehicle parking bays, EV chargers, and accessible spaces")
                        .sql("SELECT ps.id, b.name AS branch, ps.slot_number, ps.slot_type, ps.is_accessible, ps.hourly_rate, ps.is_active FROM parking_slots ps JOIN restaurant_branches b ON ps.branch_id = b.id ORDER BY b.name, ps.slot_number;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("park-bookings")
                        .module("Parking")
                        .name("Vehicle Parking Reservations")
                        .description("Booked vehicle slots with license plate number and entry code")
                        .sql("SELECT pr.id, pr.reference_no, u.email AS customer, b.name AS branch, ps.slot_number, pr.vehicle_registration_no AS license_plate, pr.vehicle_type, pr.reservation_date, pr.status FROM parking_reservations pr JOIN users u ON pr.user_id = u.id JOIN parking_slots ps ON pr.parking_slot_id = ps.id JOIN restaurant_branches b ON ps.branch_id = b.id ORDER BY pr.created_at DESC;")
                        .queryType("SELECT")
                        .build()
        ));

        // 13. System, Flyway & Table Metrics
        map.put("13. System & Database Health", List.of(
                PredefinedQueryDto.builder()
                        .id("sys-all-tables-count")
                        .module("System & Database Health")
                        .name("Database Tables & Row Counts (information_schema)")
                        .description("Shows all database tables with physical row counts and data size in MB")
                        .sql("SELECT table_name, table_rows, ROUND((data_length + index_length) / 1024 / 1024, 2) AS total_size_mb, create_time FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY table_rows DESC, table_name ASC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("sys-flyway-history")
                        .module("System & Database Health")
                        .name("Flyway Database Migration History")
                        .description("Migration version history applied to bhojana_bangalore_db")
                        .sql("SELECT installed_rank, version, description, type, script, installed_by, installed_on, execution_time, success FROM flyway_schema_history ORDER BY installed_rank ASC;")
                        .queryType("SELECT")
                        .build(),
                PredefinedQueryDto.builder()
                        .id("sys-audit-logs")
                        .module("System & Database Health")
                        .name("System Audit Trail Logs")
                        .description("Security and operational change logs recorded across the platform")
                        .sql("SELECT a.id, u.email AS actor, a.action, a.entity_type AS entity_name, a.entity_id, a.details, a.created_at FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id ORDER BY a.created_at DESC LIMIT 50;")
                        .queryType("SELECT")
                        .build()
        ));

        return map;
    }
}
