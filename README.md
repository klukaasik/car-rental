# Car Rental System

## Overview

A RESTful car rental reservation system built with **Java 21** and **Spring Boot 3.5.10**.  
Allows customers to reserve cars by type and date range, with automatic availability checking and conflict prevention.

## Features

- Reserve a car by type (Sedan, SUV, Van) at a specific date and time for a given number of days
- Real-time availability checking with overlap detection
- Input validation (past dates, invalid ranges, blank names)
- Limited fleet per car type with automatic overbooking prevention

## Tech Stack

- **Java 21** – Records, streams, modern language features
- **Spring Boot 3.5.10** – Web, Validation
- **JUnit 5** – Unit tests
- **Maven** – Build tool

## Architecture

```
Controller → Service → Repository
    ↓           ↓           ↓
  REST API  Business    In-Memory
            Logic        Storage
```

### Key Components

| Component | Responsibility | Pattern |
|-----------|---------------|---------|
| **Domain** | Core entities (`Reservation`, `CarType`) | Java Records (immutable) |
| **Repository** | Data access abstraction | Interface + in-memory implementation |
| **Service** | Business logic & validation | Plain Spring service |
| **Controller** | REST endpoints | Spring MVC |
| **DTO** | Input validation | Bean Validation (`@NotBlank`, `@NotNull`) |
| **Exception** | Error handling | `@RestControllerAdvice` |

## Project Structure

```
src/main/java/com/example/carrental/
├── domain/
│   ├── Reservation.java                    # Core entity (immutable record)
│   └── CarType.java                        # Enum: SEDAN, SUV, VAN
├── repository/
│   ├── ReservationRepository.java          # Interface
│   └── InMemoryReservationRepository.java  # In-memory implementation
├── service/
│   └── ReservationService.java             # Business logic
├── controller/
│   └── ReservationController.java          # REST API
├── dto/
│   └── CreateReservationRequest.java       # Input DTO with validation
├── exception/
│   ├── InsufficientInventoryException.java
│   └── GlobalExceptionHandler.java         # HTTP error mapping
└── config/
    └── InventoryConfig.java                # Fleet config: 2 sedans, 1 SUV, 3 vans
```

## API Endpoints

### Create Reservation

```http
POST /v1/reservations
Content-Type: application/json

{
  "customerName": "John Doe",
  "carType": "SEDAN",
  "pickupDate": "2026-03-10T10:00:00",
  "returnDate": "2026-03-15T10:00:00"
}
```

**Responses:**

| Code | Meaning |
|------|---------|
| `201 Created` | Reservation created successfully |
| `400 Bad Request` | Invalid input (past date, return before pickup, blank name) |
| `409 Conflict` | No cars of requested type available for given period |

### Check Availability

```http
GET /v1/reservations/availability?carType=SEDAN&pickupDate=2026-03-10T10:00:00&returnDate=2026-03-15T10:00:00
```

**Response:** Number of available cars of given type for the requested period.

## Running the Application

### Prerequisites

- Java 21+
- Maven 3.6+ (or use included Maven Wrapper)

### Build & Run

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run
```

Application starts on `http://localhost:8080`.

### Run Tests

```bash
./mvnw test
```

## Fleet Configuration

Configured in `InventoryConfig.java` as a Spring Bean:

| Car Type | Available Units |
|----------|----------------|
| SEDAN    | 2              |
| SUV      | 1              |
| VAN      | 3              |

## Technical Decisions

### LocalDateTime for pickup/return dates

`LocalDateTime` is used instead of `LocalDate` to support time-of-day precision for pickups and returns, allowing same-day back-to-back reservations (e.g. one customer returns at 09:00, another picks up at 10:00).

### Overlap detection algorithm

```java
existing.pickupDate().isBefore(returnDate) &&
pickupDate.isBefore(existing.returnDate())
```

Standard interval overlap check: counts how many existing reservations overlap with the requested period. If the count equals or exceeds fleet capacity for that car type, the reservation is rejected.

> **Note:** This is a conservative algorithm – it counts all overlapping reservations as concurrent usage, which may reject some edge-case valid bookings. A more precise approach would calculate peak concurrent usage per time slot. For now, this ensures overbooking never occurs.

### In-memory storage

`InMemoryReservationRepository` stores reservations in a plain `ArrayList`. This is intentional for simplicity – the `ReservationRepository` interface abstracts the storage layer, making it straightforward to swap in a JPA-backed implementation without changing any business logic.

### Thread safety

`synchronized` was intentionally omitted to keep the implementation simple. In a single-threaded or low-concurrency scenario this is sufficient. For production use, `createReservation` should be `synchronized` (or use per-carType locks for better throughput) to prevent check-then-act race conditions. A database-backed implementation would rely on pessimistic or optimistic locking instead.

### Inventory as a Spring Bean

Fleet configuration is defined as a `Map<CarType, Long>` Spring Bean in `InventoryConfig`. This separates configuration from business logic and allows easy substitution in tests by passing a different map directly to the `ReservationService` constructor.

## Known Limitations

| Limitation | Impact | Future fix |
|------------|--------|------------|
| In-memory storage | Data lost on restart | Add JPA + PostgreSQL |
| No cancellation endpoint | Inventory not freed after cancellation | Add `status` field + cancel endpoint |
| No `Clock` injection | `LocalDateTime.now()` calls untestable at exact boundary | Inject `Clock` bean |
| Conservative overlap algorithm | May reject edge-case valid bookings | Peak concurrent usage algorithm |
| No integration tests | REST layer (serialization, validation) untested end-to-end | Add MockMvc tests |
| Hardcoded fleet size | Requires recompilation to change fleet | Externalize to `application.properties` |
| No `synchronized` on `createReservation` | Race condition possible under concurrent load | Add `synchronized` or DB-level locking |

## Unit Tests

Tests are located in `ReservationServiceTest` and cover:

- Successful reservation creation
- Rejection of past pickup dates
- Rejection of invalid date ranges (return before or equal to pickup)
- Rejection when fleet capacity is exceeded
- Allowing multiple concurrent bookings within fleet capacity
- Correct availability count with overlapping reservations
- Correct availability count with non-overlapping reservations
