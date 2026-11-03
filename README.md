# 🍽️ RestaurantHub

### Dining Reservations, Private Rooms, Party Halls & Food Delivery

RestaurantHub is a web-based restaurant management platform that allows customers to reserve dining tables, private rooms, party halls, parking slots, and order food online with delivery tracking.

## ✨ Features

* **Dining Reservations:** Book tables with real-time availability.
* **Private Rooms:** Reserve private dining rooms.
* **Party Halls:** Book halls for events and celebrations.
* **Parking Reservations:** Reserve parking slots.
* **Online Food Ordering:** Browse menus and order food online.
* **Delivery Tracking:** Track food orders through different delivery stages.
* **Authentication & Authorization:** Secure login with role-based access.
* **Admin Dashboard:** Manage restaurant operations, bookings, orders, and inventory.
* **Payment Integration:** Support for payment gateway integration.

## 🛠️ Tech Stack

| Technology      | Description                    |
| --------------- | ------------------------------ |
| Java 17         | Backend Programming            |
| Spring Boot 3   | Backend Framework              |
| Spring Security | Authentication & Authorization |
| Spring Data JPA | Database Operations            |
| MySQL 8         | Database                       |
| Flyway          | Database Migrations            |
| Thymeleaf       | Frontend Templates             |
| Bootstrap 5     | UI Styling                     |
| Maven           | Build Management               |
| Docker          | Containerization               |

## 🚀 Installation & Setup

### Prerequisites

* JDK 17+
* MySQL 8+
* Maven 3.8+

### 1. Clone the Repository

```bash
git clone <your-repository-url>
cd bhojana-bangalore
```

### 2. Configure the Database

Create the database in MySQL:

```sql
CREATE DATABASE bhojana_bangalore_db;
```

Configure your database credentials in `application-local.yml` or the appropriate application configuration file.

### 3. Run the Application

```bash
mvn clean spring-boot:run
```

### 4. Access the Application

* **Web Application:** http://localhost:8080
* **Swagger API Documentation:** http://localhost:8080/swagger-ui.html

## 📂 Project Modules

* Authentication & User Management
* Restaurant & Branch Management
* Dining Table Reservations
* Private Room & Party Hall Bookings
* Parking Management
* Menu & Food Ordering
* Payment Management
* Delivery Tracking
* Admin Dashboard

## 🔐 User Roles

* Admin
* Manager
* Staff
* Kitchen
* Customer

## 📄 License

This project is licensed under the MIT License.
