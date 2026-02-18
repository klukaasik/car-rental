# Simple Car Rental System - Development Plan
## 3-Hour Technical Exercise - SIMPLIFIED

**Target:** Java 21 + Spring Boot  
**Time Budget:** 3 hours (strict)  
**Scope:** Minimal viable system - core requirements ONLY  
**Storage:** In-memory (no database)

---

## 1. Requirements Analysis - MUST HAVE ONLY

### 1.1 Core Requirements (Non-Negotiable)
✅ Reserve a car by **type** (Sedan, SUV, Van) for **date range** (N days)  
✅ Each car type has **limited inventory** (fixed quantity)  
✅ Prevent **double-booking** (no overlapping reservations exceeding inventory)  
✅ **Unit tests** proving the system works correctly  
✅ **In-memory storage** (no database setup)

### 1.2 Key Design Decisions

| Decision | Rationale | Time Saved |
|----------|-----------|------------|
| **Day granularity** | Pickup/return dates only (no times) | Simplifies date logic, no timezone issues |
| **Pool-based inventory** | Count per type, not individual cars | No need for car ID tracking |
| **No customer entity** | Store name as string in reservation | Skip user management layer |
| **No cancellation** | Create-only reservations | Eliminates status management |
| **Simple availability check** | Linear scan of reservations | No complex date indexing |
| **Synchronized service** | Coarse-grained locking | Simple thread-safety |

### 1.3 Critical Edge Cases (Must Handle)

| Edge Case | Validation Rule |
|-----------|-----------------|
| **Past date reservation** | `pickupDate >= LocalDate.now()` |
| **Return before pickup** | `returnDate > pickupDate` |
| **Zero duration** | `returnDate > pickupDate` (same check) |
| **Insufficient inventory** | Check availability before saving |
| **Overlapping bookings** | Count concurrent reservations |
| **Null inputs** | Validate all parameters, throw `IllegalArgumentException` |

**Explicitly NOT handling in 3 hours:**
- Email format validation
- Idempotent operations
- Partial day calculations
- Time zones (use LocalDate.now() with system default)

---

## 2. Domain Model - MINIMAL DESIGN

### 2.1 Domain Objects (3 classes/enums total)

#### CarType (Enum)
```java
public enum CarType {
    SEDAN,
    SUV,
    VAN
}
```
**Why:** Fixed set, type-safe, zero logic needed.

#### Reservation (Record)
```java
public record Reservation(
    String id,              // UUID
    String customerName,    // No separate Customer entity
    CarType carType,
    LocalDate pickupDate,   // Inclusive
    LocalDate returnDate    // Exclusive
) {
    // Single helper method for validation
    public long getDurationDays() {
        return pickupDate.until(returnDate, ChronoUnit.DAYS);
    }
}
```
**Why Record:** Immutable, minimal boilerplate, perfect for domain data.  
**No Status Enum:** All reservations are active (no cancellation).  
**No Email:** Removed to save time, name is sufficient.  
**No createdAt:** Not required for core functionality.

#### InventoryConfig (Simple Map)
```java
// Just use Map<CarType, Integer> directly in configuration
// No need for a separate record wrapper
```
**Why:** One less class to maintain. Inject map directly into service.

---

## 3. Architecture - 3 LAYERS ONLY

### 3.1 Package Structure (Minimal)

```
com.example.carrental/
├── domain/
│   ├── Reservation.java        (record)
│   └── CarType.java            (enum)
├── repository/
│   └── ReservationRepository.java
├── service/
│   └── ReservationService.java
├── config/
│   └── InventoryConfiguration.java
└── exception/
    └── InsufficientInventoryException.java
```

**Total: ~6 files** (excluding tests)

### 3.2 Minimal Repository Interface

```java
public interface ReservationRepository {
    Reservation save(Reservation reservation);
    List<Reservation> findByCarTypeAndDateRange(CarType carType, 
                                                 LocalDate start, 
                                                 LocalDate end);
}
```

**Only 2 methods needed!**  
- `save()` - create new reservation
- `findByCarTypeAndDateRange()` - check overlaps

**Removed (not needed for core):**
- ❌ `findById()` - no lookup by ID required
- ❌ `findByCustomerEmail()` - no customer queries
- ❌ `findAll()` - not used
- ❌ `update()` - no cancellation/modification

### 3.3 Minimal Service Interface

```java
public class ReservationService {
    public Reservation createReservation(String customerName,
                                        CarType carType,
                                        LocalDate pickupDate,
                                        LocalDate returnDate);
    
    public int checkAvailability(CarType carType,
                                 LocalDate pickupDate,
                                 LocalDate returnDate);
}
```

**Core responsibilities:**
1. Validate inputs (nulls, past dates, date range)
2. Check availability (count overlaps)
3. Save if available, throw exception if not

### 3.4 Storage Implementation

```java
@Repository
public class ReservationRepository {
    // Single concurrent collection
    private final List<Reservation> reservations = 
        new CopyOnWriteArrayList<>();
    
    // Implementation will filter with streams - simple & readable
}
```

**Why CopyOnWriteArrayList:**
- Thread-safe for reads (most operations)
- Simple to implement
- Good enough for <100 reservations
- No need for ConcurrentHashMap complexity

---

## 4. Testing Strategy - FOCUSED TEST SUITE

### 4.1 Core Test Cases (9 tests maximum)

#### ReservationServiceTest (7 essential tests)

| # | Test Name | Validates |
|---|-----------|-----------|
| 1 | `shouldCreateReservation_WhenAvailable` | Happy path works |
| 2 | `shouldThrowException_WhenPickupDateInPast` | No past date bookings |
| 3 | `shouldThrowException_WhenInvalidDateRange` | Return > pickup |
| 4 | `shouldThrowException_WhenInsufficientInventory` | No overbooking |
| 5 | `shouldAllowOverlap_WhenInventoryAvailable` | Multiple reservations same dates if capacity exists |
| 6 | `shouldCountOverlappingReservations_Correctly` | Availability logic correct |
| 7 | `shouldValidateNullInputs` | Defensive programming |

#### Integration Test (2 tests)

| # | Test Name | Validates |
|---|-----------|-----------|
| 8 | `shouldIntegrateServiceAndRepository` | Full flow end-to-end |
| 9 | `shouldHandleMultipleCarTypes` | Sedan/SUV/Van independent |

**Total: 9 tests = sufficient for 3-hour scope**

### 4.2 Test Data Setup

```java
// Simple test data constants
private static final LocalDate TODAY = LocalDate.now();
private static final LocalDate TOMORROW = TODAY.plusDays(1);
private static final LocalDate IN_3_DAYS = TODAY.plusDays(3);
private static final LocalDate IN_5_DAYS = TODAY.plusDays(5);

private static final Map<CarType, Integer> TEST_INVENTORY = Map.of(
    CarType.SEDAN, 2,  // Only 2 sedans for easy testing
    CarType.SUV, 1,    // 1 SUV - edge case testing
    CarType.VAN, 3
);
```

### 4.3 What NOT to Test (Out of Scope)

- ❌ Concurrency stress tests (`@RepeatedTest(100)`) - nice to have
- ❌ Repository unit tests separate from service - integration is enough
- ❌ Domain object unit tests - records are trivial
- ❌ Test builders/factories - direct constructors are fine
- ❌ Mocking - use real repository instance for simplicity

---

## 5. Availability Logic - SIMPLIFIED ALGORITHM

### 5.1 Simple Availability Check (Good Enough™)

**Question:** Can I reserve this car type for these dates?

**Algorithm:**
```java
public int checkAvailability(CarType carType, LocalDate pickup, LocalDate return) {
    int totalCars = inventory.get(carType);
    
    // Find all reservations that overlap with requested dates
    List<Reservation> overlapping = repository.findByCarTypeAndDateRange(
        carType, pickup, return
    );
    
    int reserved = overlapping.size();
    return totalCars - reserved;
}
```

**Assumption:** Each reservation reserves exactly 1 car.  
**Limitation:** Doesn't find "peak day" usage - may be conservative.  
**Why it works:** For 3-hour scope, simple is better than perfect.

### 5.2 Overlap Detection (Standard Algorithm)

```java
private boolean overlaps(Reservation existing, LocalDate start, LocalDate end) {
    // Two ranges overlap if: start1 < end2 AND start2 < end1
    return existing.pickupDate().isBefore(end) && 
           start.isBefore(existing.returnDate());
}
```

**Visual:**
```
Requested:     [---R---]
Existing A:  [---A---]     → OVERLAP (A.pickup < R.return && R.pickup < A.return)
Existing B:              [---B---]  → NO OVERLAP
```

### 5.3 Thread Safety (Minimal)

```java
@Service
public class ReservationService {
    // Simplest thread-safety: synchronized method
    public synchronized Reservation createReservation(...) {
        // Check + save in single atomic operation
    }
}
```

**Trade-off:** Coarse lock = simple but lower throughput. Good for 3-hour scope.

---

## 6. Implementation Timeline - 3 HOURS REALISTIC

### 6.1 Time-Boxed Phases

| Phase | Time | Deliverable | Key Activities |
|-------|------|-------------|----------------|
| **1. Domain** | 20 min | Enums + Record | `CarType`, `Reservation` with validation helper |
| **2. Repository** | 25 min | Interface + Impl | 2 methods, CopyOnWriteArrayList, 2 tests |
| **3. Service** | 45 min | Business logic | Availability check, input validation, 4 tests |
| **4. Integration** | 30 min | Full flow | Wire with Spring Boot config, 2 integration tests |
| **5. Polish** | 40 min | Clean up | Javadoc, exception messages, run all tests |
| **6. Buffer** | 20 min | Contingency | Debugging, refactoring |

**Total: 180 minutes (3 hours)**

### 6.2 Implementation Checklist

**Phase 1: Domain (20 min)**
- [ ] Create `CarType` enum (5 min)
- [ ] Create `Reservation` record with `getDurationDays()` (10 min)
- [ ] Write 1 test for date validation (5 min)

**Phase 2: Repository (25 min)**
- [ ] Define `ReservationRepository` interface (5 min)
- [ ] Implement with `CopyOnWriteArrayList` (10 min)
- [ ] Write 2 repository tests (10 min)

**Phase 3: Service (45 min)**
- [ ] Create `InsufficientInventoryException` (3 min)
- [ ] Implement `createReservation()` with validation (15 min)
- [ ] Implement `checkAvailability()` with overlap logic (12 min)
- [ ] Write 5 service tests (15 min)

**Phase 4: Integration (30 min)**
- [ ] Create `InventoryConfiguration` bean (5 min)
- [ ] Wire service with Spring dependency injection (5 min)
- [ ] Write 2 integration tests with `@SpringBootTest` (15 min)
- [ ] Run all tests, verify green (5 min)

**Phase 5: Polish (40 min)**
- [ ] Add JavaDoc to public methods (10 min)
- [ ] Improve exception messages (5 min)
- [ ] Review code for cleanup (10 min)
- [ ] Final test run + coverage check (5 min)
- [ ] Quick demo scenario in test (10 min)

### 6.3 Maven Commands

```bash
# Run tests (do this frequently!)
mvn test

# Skip if time constrained: coverage report
mvn test jacoco:report

# NOT needed for this exercise (no REST API)
mvn spring-boot:run
```

---

## 7. OUT OF SCOPE - Nice to Have (NOT in 3 hours)

### 7.1 Explicitly Excluded Features

| Feature | Why Excluded | Time Saved |
|---------|--------------|------------|
| **Reservation cancellation** | Not in core requirements | 30 min |
| **Customer entity / email queries** | Use simple string name | 20 min |
| **Email format validation** | Not business-critical | 5 min |
| **REST Controller / API** | Tests prove correctness | 30-45 min |
| **Pricing calculation** | Not mentioned in requirements | 15 min |
| **Per-day peak usage algorithm** | Simple overlap check sufficient | 20 min |
| **Concurrency stress testing** | Basic synchronized is enough | 15 min |
| **JaCoCo coverage report** | Nice to have, not required | 5 min |
| **Advanced repository queries** | Only 2 methods needed | 15 min |
| **Separate InventoryConfig record** | Use Map directly | 5 min |
| **Test builders / factories** | Direct constructors simpler | 10 min |
| **JavaDoc on everything** | Only public API needs docs | 10 min |

**Total time saved: ~180 minutes** = Fits in 3-hour window!

### 7.2 What Could Be Added Next (If More Time)

**Priority 1 (15-30 min each):**
- Reservation cancellation with status tracking
- REST controller for external access
- Customer entity with email validation

**Priority 2 (30-60 min each):**
- Pricing calculation (daily rate × days)
- Modification (extend dates)
- Email notifications

**Priority 3 (Advanced):**
- Individual car tracking (not just pools)
- Multi-location support
- Maintenance windows
- Reporting/analytics

---

## 8. Success Criteria - What "Done" Looks Like

### 8.1 Functional Requirements ✅

- [x] Can create reservations by car type and date range
- [x] Validates input (no past dates, invalid date ranges, nulls)
- [x] Respects inventory limits (no overbooking)
- [x] Handles overlapping reservations correctly
- [x] Thread-safe (synchronized service method)

### 8.2 Code Quality ✅

- [x] Clean, readable code (meaningful names)
- [x] Proper layering (domain/repository/service/config)
- [x] 9 tests covering core scenarios
- [x] No compiler warnings
- [x] Domain model independent of Spring

### 8.3 Deliverables ✅

- [x] Working system with all tests passing
- [x] Can explain design decisions
- [x] Can discuss trade-offs and limitations
- [x] Ready to demo via unit tests

---

## 9. Key Design Principles Applied

### 9.1 SOLID in 3 Hours

| Principle | How Applied |
|-----------|-------------|
| **Single Responsibility** | Repository = storage, Service = business logic |
| **Open/Closed** | Repository interface allows swapping implementations |
| **Dependency Inversion** | Service depends on Repository interface, not concrete class |

### 9.2 Code Patterns

- **Immutability:** Records for domain objects
- **Fail Fast:** Validate inputs immediately, throw exceptions
- **Separation of Concerns:** Domain independent of framework
- **Testability:** Constructor injection, no statics
- **Simplicity:** Prefer simple solutions over clever ones

---

## 10. Quick Reference - Implementation Summary

### 10.1 Files to Create (~6 files)

```
domain/
  ├── CarType.java              (3 enum values)
  └── Reservation.java          (5 fields + 1 helper method)
repository/
  └── ReservationRepository.java (interface + impl in 1 file)
service/
  └── ReservationService.java   (2 public methods)
config/
  └── InventoryConfiguration.java (1 @Bean method)
exception/
  └── InsufficientInventoryException.java (extends RuntimeException)
```

### 10.2 Core Logic (Pseudo-code)

```java
// Service.createReservation()
1. Validate inputs (nulls, pickup date >= today, date range)
2. Check availability = totalCars - countOverlapping()
3. If available > 0: save reservation
4. Else: throw InsufficientInventoryException

// Repository.findByCarTypeAndDateRange()
return allReservations.stream()
    .filter(matches car type)
    .filter(overlaps date range)
    .toList();

// Overlap check
return r.pickupDate < requestEnd && requestStart < r.returnDate;
```

### 10.3 Configuration

```java
@Bean
public Map<CarType, Integer> inventory() {
    return Map.of(
        CarType.SEDAN, 5,
        CarType.SUV, 3,
        CarType.VAN, 2
    );
}
```

**Storage:** Single `CopyOnWriteArrayList<Reservation>` - thread-safe, simple.

---

## Final Checklist Before Starting

- [ ] Understand core requirement: reserve car by type for date range
- [ ] Know what to skip: cancellation, customer entity, REST API, pricing
- [ ] Have 3 hours blocked without interruptions
- [ ] IDE ready (IntelliJ/Eclipse), Maven configured
- [ ] Ready to TDD: write test first, make it pass, refactor
- [ ] Remember: **simple & correct > complex & buggy**

---

**Time to Code!** 🚗💨

Focus on the 6 essential files, 9 core tests, and delivering a working system that proves the requirements. Everything else is noise.
