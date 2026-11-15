package com.restauranthub.service.sql;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        seedDemoDataIfEmpty();
    }

    public synchronized void seedDemoDataIfEmpty() {
        try {
            Integer orderCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
            if (orderCount == null || orderCount == 0) {
                log.info("No orders found in MySQL. Automatically seeding rich Bangalore demo data...");
                executeSeedScript();
            } else {
                log.info("MySQL bhojana_bangalore_db already contains {} orders. Checking delivery tracking...", orderCount);
                Integer dtCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM delivery_tracking", Integer.class);
                if (dtCount == null || dtCount == 0) {
                    log.info("Backfilling delivery_tracking records for existing orders...");
                    jdbcTemplate.execute(
                            "INSERT INTO delivery_tracking (order_id, status, current_location, driver_name, vehicle_number, status_updated_at, created_at, updated_at) " +
                            "SELECT id, status, 'Spice Garden Kitchen, MG Road', 'Kiran Gowda', 'KA-01-EQ-4921', updated_at, created_at, updated_at " +
                            "FROM orders WHERE id NOT IN (SELECT order_id FROM delivery_tracking);"
                    );
                }

                // Ensure Veg Spring Rolls image points to authentic crispy spring rolls
                try {
                    jdbcTemplate.update(
                            "UPDATE menu_items SET image_url = 'https://images.unsplash.com/photo-1515022376298-7333f33e704b?w=800' " +
                            "WHERE (name LIKE '%Spring Roll%' OR id = 101) AND (image_url LIKE '%photo-1548802673%' OR image_url IS NULL);"
                    );
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Auto demo seed check encountered exception (schema might be initialising): {}", e.getMessage());
        }
    }

    public synchronized void forceSeedDemoData() {
        log.info("Force executing Bangalore rich demo data seed script...");
        executeSeedScript();
    }

    private void executeSeedScript() {
        try (Connection conn = dataSource.getConnection()) {
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            if (dbProduct != null && !dbProduct.toLowerCase().contains("mysql")) {
                log.info("Non-MySQL database detected ({}) - skipping MySQL-specific seed script during tests.", dbProduct);
                return;
            }
            ClassPathResource scriptResource = new ClassPathResource("db/migration/V6__seed_rich_bangalore_demo_data.sql");
            if (scriptResource.exists()) {
                ScriptUtils.executeSqlScript(conn, scriptResource);
                log.info("Successfully executed V6__seed_rich_bangalore_demo_data.sql in MySQL");
            } else {
                log.warn("Migration script V6__seed_rich_bangalore_demo_data.sql not found on classpath");
            }
        } catch (Exception ex) {
            log.error("Failed to execute seed script: {}", ex.getMessage(), ex);
            throw new RuntimeException("Demo seed execution failed: " + ex.getMessage(), ex);
        }
    }
}
