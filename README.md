# RestaurantHub — Restaurant Management, Concurrency-Safe Reservation & Online Food Ordering Platform

[![Java](https://img.shields.io/badge/Java-17%20%7C%2021%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x%20(Dual%20Auth)-blue.svg)](https://spring.io/projects/spring-security)
[![Database](https://img.shields.io/badge/MySQL-8.x-blue.svg)](https://www.mysql.com/)
[![Flyway](https://img.shields.io/badge/Flyway-10.x%20Migrations-red.svg)](https://flywaydb.org/)
[![UI](https://img.shields.io/badge/Frontend-Thymeleaf%20%2B%20Bootstrap%205-purple.svg)](https://getbootstrap.com/)

RestaurantHub is an enterprise-grade, production-oriented monolithic platform that unifies restaurant table reservations, luxury private dining rooms, banquet party halls, parking bay bookings, and full-lifecycle online food ordering into a single cohesive platform.

Built with clean layered architecture, strict server-side validation, role-based access control, pessimistic database locking to eliminate booking collisions, and automated payment/inventory workflows.

---

## ⚠️ Important: Resolution for IntelliJ IDEA Compiler Error

If you encountered this compilation error in IntelliJ IDEA:
```text
java: java.lang.ExceptionInInitializerError
com.sun.tools.javac.code.TypeTag :: UNKNOWN
```

### Why this happened
When using **Java 21 or Java 22** with an older version of Project Lombok (versions `< 1.18.30`), Lombok's annotation processor fails to inspect the JDK's internal `com.sun.tools.javac.code.TypeTag` enum because the Java compiler team modified type tags in newer JDK releases.

### How it has been resolved in this codebase
1. **Lombok 1.18.38** has been configured in `pom.xml` under `<properties>`, the explicit `<dependency>` declaration, and the `maven-compiler-plugin`'s `<annotationProcessorPaths>`. In addition, `.idea/compiler.xml` has been updated to point to Lombok 1.18.38. Version 1.18.38 officially supports JDK 17, 21, 22, 23, and 24.
2. In IntelliJ IDEA, apply the following quick steps:
   1. **Reload Maven Project**:
      - Open the **Maven** tool window on the right sidebar (or right-click `pom.xml` ➔ **Maven** ➔ **Reload Project**). This downloads `lombok-1.18.38.jar`.
   2. **Rebuild Project**:
      - Navigate to top menu: **Build** ➔ **Rebuild Project** (or press `Ctrl+F9` / `Cmd+F9`).
   3. *(Optional / Recommended)*: In **Settings** ➔ **Build, Execution, Deployment** ➔ **Build Tools** ➔ **Maven** ➔ **Runner**, check **"Delegate IDE build/run actions to Maven"**. This guarantees that all builds run through standard Maven and prevents IDE compiler version mismatches.

The project will now compile cleanly with zero warnings or `TypeTag` errors on JDK 17, 21, 22, 23, and 24.

---

## 1. Key Platform Capabilities

### 🪑 Multi-Resource Reservation Engine
- **Dining Tables**: Real-time availability check, section filtering (Main Dining, Patio, Terrace, Bar), guest capacity validation, and automatic table assignment.
- **VIP Dining Suites**: Hourly rate models, minimum duration rules, luxury amenity tracking, and deposit requirements.
- **Banquet Party Halls**: Large event booking with configurable catering packages (Silver, Gold, Platinum).
- **Parking Bays**: Vehicle classifications (Standard Car, EV Charging, Motorcycle, Oversized Van) with contactless entry reference codes.
- **Zero Double-Booking Guarantee**: Employs `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`) on parent resources combined with overlapping interval queries and 15-minute temporary hold expirations.

### 🍕 Online Food Ordering & Inventory Management
- **Catalog**: Categorized menus (Appetizers, Mains, Steaks, Pasta, Desserts, Beverages) with item variants and optional addons.
- **Real-Time Cart**: Server-side subtotal, discount, tax (8.25%), and delivery calculations. Client-submitted prices are strictly discarded.
- **Promotions & Coupons**: Minimum spend rules, fixed and percentage discounts, per-user redemption limits, and validity checks.
- **Atomic Stock Deduction**: Order placement atomically decrements inventory with pessimistic locking. Insufficient stock immediately blocks order submission.
- **Order State Machine**: Enforces valid status transitions:
  `PENDING_PAYMENT` ➔ `CONFIRMED` ➔ `PLACED` ➔ `ACCEPTED` ➔ `PREPARING` ➔ `READY_FOR_PICKUP` ➔ `OUT_FOR_DELIVERY` ➔ `COMPLETED` (or `CANCELLED` / `REFUNDED`).

### 💳 Payment & Webhook Integration
- Pluggable gateway architecture (`PaymentGatewayService`):
  - **MOCK**: Integrated for seamless local development and automated testing (auto-approves payment).
  - **STRIPE**: Card processing with signature-verified webhooks (`StripePaymentGatewayService`).
  - **RAZORPAY**: Order creation and signature validation (`RazorpayPaymentGatewayService`).
- Full refund processing and idempotent webhook event logging to prevent duplicate credit entries.

### 🛡️ Dual-Layer Security & RBAC
- **Web UI**: Stateful session-based authentication with CSRF protection.
- **REST APIs**: Stateless JWT Bearer token authentication (`Authorization: Bearer <token>`).
- Strict Role Hierarchy: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STAFF`, `ROLE_KITCHEN`, `ROLE_CUSTOMER`.
- Anti-IDOR protections: Users can only inspect and manage their own orders, bookings, and profile data.

---

## 2. Technology Stack

| Layer | Technologies |
|---|---|
| **Backend Framework** | Spring Boot 3.3.4, Java 17 / 21 LTS |
| **Persistence & ORM** | Spring Data JPA, Hibernate 6, MySQL Connector/J |
| **Database Migrations**| Flyway 10.x (Versioned SQL migrations) |
| **Security & Auth** | Spring Security 6, JJWT 0.12.6, BCrypt (strength 10) |
| **API Documentation** | Springdoc OpenAPI 2.6.0 (Swagger UI 3.x) |
| **Frontend Templates** | Thymeleaf 3, Thymeleaf Spring Security 6 Extras |
| **Styling & Icons** | Bootstrap 5.3, FontAwesome 6, Google Fonts |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 (test profile) |
| **Containerization** | Docker, Multi-stage Dockerfile, Docker Compose |

---

## 3. Project Structure

```text
com.restauranthub
├── config/                 # Spring MVC, OpenAPI, and Web configurations
├── controller/             # Web controllers (Thymeleaf) & REST controllers (/api/v1/**)
│   ├── AuthRestController.java & AuthWebController.java
│   ├── BranchRestController.java & BranchWebController.java
│   ├── ReservationRestController.java & ReservationWebController.java
│   ├── MenuRestController.java, CartRestController.java, OrderRestController.java
│   ├── PaymentRestController.java, AdminReportRestController.java
│   ├── AdminWebController.java, KitchenWebController.java, HomeController.java
├── dto/                    # Strict request and response data transfer objects
├── entity/                 # Normalized JPA entity models with audit timestamps
│   └── enums/              # ReservationStatus, OrderStatus, TableType, VehicleType, etc.
├── exception/              # ApiException, ResourceNotFoundException, BookingConflictException, etc.
│   └── GlobalExceptionHandler.java (Dual JSON and HTML error rendering)
├── notification/           # In-app notifications & fault-tolerant @Async email dispatcher
├── payment/                # PaymentGatewayService, Mock, Stripe, and Razorpay implementations
├── repository/             # Spring Data JPA repositories with pessimistic lock queries
├── scheduler/              # Automated scheduled worker for releasing expired 15-min holds
├── security/               # JwtTokenProvider, JwtAuthenticationFilter, SecurityConfig
├── service/                # Transactional domain logic, state machines, and calculations
└── util/                   # Common helper utilities
```

---

## 4. Default Seed Accounts & Credentials

The application automatically seeds the following accounts via Flyway (`V2__seed_initial_data.sql`).

> **Default password for all accounts:** `password`

| Email Address | Role | Description / Permissions |
|---|---|---|
| `admin@restauranthub.com` | `ROLE_ADMIN` | Global platform administration, revenue metrics, audit review, branch setup. |
| `manager@restauranthub.com` | `ROLE_MANAGER` | Branch management, menu pricing, inventory replenishment, operational reports. |
| `staff@restauranthub.com` | `ROLE_STAFF` | Floor check-ins, table management, booking updates, parking validation. |
| `kitchen@restauranthub.com` | `ROLE_KITCHEN` | Kitchen live queue, chef order fulfillment, preparation state transitions. |
| `customer@restauranthub.com` | `ROLE_CUSTOMER` | Public dining customer, multi-resource reservations, cart checkout, tracking. |

---

## 5. Prerequisites & Quick Start

### Prerequisites
- **JDK 17** or **JDK 21** installed and configured in `JAVA_HOME`.
- **MySQL 8.x** running locally (or use Docker).
- **Maven 3.8+** (or use the included Maven wrapper).

---

### Method A: Local Run with MySQL

1. **Create the Database in MySQL**:
   ```sql
   CREATE DATABASE restauranthub_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **Verify Database Credentials**:
   Inspect or edit `src/main/resources/application-local.yml` (default: user `root`, password `rootpassword` or configure your environment variables):
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/restauranthub_db?createDatabaseIfNotExist=true...
       username: root
       password: rootpassword
   ```
   *(Or set shell variables: `set DB_PASSWORD=your_password` in Windows Command Prompt, or `$env:DB_PASSWORD="your_password"` in PowerShell).*

3. **Build & Run**:
   ```bash
   mvn clean spring-boot:run
   ```
   Flyway will automatically execute migrations:
   - `V1__init_schema.sql` (Creates 40 normalized tables with indexes and constraints).
   - `V2__seed_initial_data.sql` (Seeds branches, tables, VIP rooms, halls, parking bays, menu items, coupons, and user accounts).

4. **Access the Application**:
   - Web Portal: [http://localhost:8080](http://localhost:8080)
   - Interactive Swagger API Docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - OpenAPI Raw Spec: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

### Method B: Run with Docker Compose

To spin up both MySQL 8 and RestaurantHub in isolated containers:
```bash
docker-compose up --build -d
```
To stop the stack:
```bash
docker-compose down
```

---

## 6. Running Automated Tests

RestaurantHub includes comprehensive unit, integration, security, and concurrency test suites that run against an in-memory H2 database (zero dependency on local MySQL):

```bash
mvn clean test
```

### Key Test Suites Included:
1. **`RestaurantHubApplicationTests`**: Validates Spring ApplicationContext bootstrapping and bean wiring.
2. **`SecurityAuthorizationTest`**: Verifies role-based access control (RBAC), anonymous endpoint access, 401 Unauthorized rejections, and 403 Forbidden checks for non-admin roles.
3. **`TableReservationConcurrencyTest`**: Tests pessimistic locking and verifies that overlapping booking attempts throw `BookingConflictException`.
4. **`OrderServiceTest`**: Validates cart-to-order conversion, atomic inventory deduction, out-of-stock rejection, and strict order state machine progression.
5. **`AuthServiceTest`**: Validates customer registration, duplicate email rejection, BCrypt hashing, and JWT token generation upon login.

---

## 7. Web Application Sitemap & URLs

| Category | URL | Description | Required Role |
|---|---|---|---|
| **Public** | `/` | RestaurantHub Homepage with highlights & quick booking | Public / Any |
| | `/branches` | Browse restaurant branches & facilities | Public / Any |
| | `/branches/{id}` | Detailed branch view, hours, and menus | Public / Any |
| | `/menu` | Food ordering menu with categories & search | Public / Any |
| | `/login` | Authentication page | Public / Any |
| | `/register` | Customer sign-up page | Public / Any |
| **Customer Portal** | `/cart` | Active shopping cart with discount coupons | Customer |
| | `/checkout` | Order review, delivery address & payment | Customer |
| | `/orders/my-orders` | Historical & active food orders | Customer |
| | `/orders/{orderNumber}/track` | Live delivery / order tracking view | Customer |
| | `/reservations/tables/new` | Dining table booking form | Customer |
| | `/reservations/rooms/new` | VIP Private Dining Suite booking form | Customer |
| | `/reservations/halls/new` | Banquet Party Hall booking form | Customer |
| | `/reservations/parking/new` | Parking Slot reservation form | Customer |
| | `/reservations/my-bookings` | Unified multi-tab dashboard for all bookings | Customer |
| | `/profile` | Profile details & saved addresses | Customer |
| **Kitchen Station** | `/kitchen/queue` | Live kitchen intake queue with status buttons | Kitchen, Manager, Admin |
| **Administration** | `/admin/dashboard` | Executive KPI cards, revenue metrics, stock alerts | Admin |

---

## 8. REST API Reference & Postman Collection

A pre-configured Postman collection is located at the root of the project:
`RestaurantHub.postman_collection.json`

### Importing into Postman:
1. Open Postman ➔ Click **Import** (top left).
2. Select `RestaurantHub.postman_collection.json`.
3. The collection includes environment variables:
   - `baseUrl`: `http://localhost:8080`
   - `jwt_token`: Automatically captured from the test script of the **Login Customer** request.

### Core REST Endpoints Overview:
- `POST /api/v1/auth/register` — Register customer account.
- `POST /api/v1/auth/login` — Authenticate and receive JWT Bearer token.
- `GET  /api/v1/branches` — Search branches and filter by amenities.
- `POST /api/v1/reservations/tables` — Reserve table with concurrency locking.
- `POST /api/v1/reservations/rooms` — Reserve private VIP suite.
- `POST /api/v1/reservations/halls` — Book banquet party hall with catering package.
- `POST /api/v1/reservations/parking` — Reserve parking slot with vehicle type.
- `GET  /api/v1/menu/items` — Fetch menu catalog with prices and variants.
- `POST /api/v1/cart/items` — Add items with addons to cart.
- `POST /api/v1/cart/coupon` — Apply promotional coupon code.
- `POST /api/v1/orders` — Place order and deduct inventory atomically.
- `POST /api/v1/payments/process` — Process deposit or order payment.
- `POST /api/v1/payments/webhooks/{provider}` — Idempotent webhook receiver.
- `GET  /api/v1/admin/reports/dashboard` — Platform executive KPIs (Admin only).

---

## 9. Security & Concurrency Design Highlights

1. **Zero Double-Booking Guarantee**:
   MySQL lacks exclusion constraints for overlapping intervals. RestaurantHub solves this by issuing:
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT t FROM DiningTable t WHERE t.id = :id")
   Optional<DiningTable> findByIdWithLock(@Param("id") Long id);
   ```
   Within a `@Transactional(isolation = Isolation.REPEATABLE_READ)` boundary, this holds an exclusive row-level lock on the table/room/hall/slot, evaluates the buffer-adjusted overlap query, and records the reservation before releasing the lock.

2. **15-Minute Temporary Holds**:
   Unpaid reservations expire automatically after 15 minutes. The background scheduler (`ReservationExpirationScheduler`) runs every 5 minutes to release holds and restore resource availability.

3. **Financial Protection**:
   All monetary calculations (subtotal, percentage/fixed discounts, 8.25% state sales tax, delivery fees) are strictly evaluated server-side using `BigDecimal` with `RoundingMode.HALF_UP`. Client-supplied totals are discarded.

4. **Audit Logging & Notifications**:
   Sensitive operations (status transitions, cancellations, refunds, inventory changes) log audit trails with user IDs and timestamps. Asynchronous email notifications (`@Async`) keep guests informed without blocking request threads.

---

## 10. License

This project is licensed under the MIT License. Developed for enterprise restaurant operations and mission-critical food ordering platforms.
