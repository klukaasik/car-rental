package com.example.carrental.service;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.exception.InsufficientInventoryException;
import com.example.carrental.repository.InMemoryReservationRepository;
import com.example.carrental.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceTest {

    private ReservationService service;
    private ReservationRepository repository;
    private Map<CarType, Long> testInventory;

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final LocalDateTime TOMORROW = NOW.plusDays(1);
    private static final LocalDateTime IN_3_DAYS = NOW.plusDays(3);
    private static final LocalDateTime IN_5_DAYS = NOW.plusDays(5);
    private static final LocalDateTime YESTERDAY = NOW.minusDays(1);

    @BeforeEach
    void setUp() {
        testInventory = Map.of(
                CarType.SEDAN, 2L,
                CarType.SUV, 1L,
                CarType.VAN, 3L
        );
        repository = new InMemoryReservationRepository();
        service = new ReservationService(repository, testInventory);
    }

    @Test
    void shouldCreateReservation_WhenAvailable() {
        // Given
        String customerName = "Jan Nowak";
        CarType carType = CarType.SEDAN;
        LocalDateTime pickupDate = TOMORROW;
        LocalDateTime returnDate = IN_3_DAYS;

        // When
        Reservation reservation = service.createReservation(customerName, carType, pickupDate, returnDate);

        // Then
        assertNotNull(reservation);
        assertNotNull(reservation.id());
        assertEquals(customerName, reservation.customerName());
        assertEquals(carType, reservation.carType());
        assertEquals(pickupDate, reservation.pickupDate());
        assertEquals(returnDate, reservation.returnDate());
    }

    @Test
    void shouldThrowException_WhenPickupDateInPast() {
        // Given
        String customerName = "Jan Nowak";
        CarType carType = CarType.SUV;
        LocalDateTime pickupDate = YESTERDAY;
        LocalDateTime returnDate = TOMORROW;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createReservation(customerName, carType, pickupDate, returnDate)
        );

        assertTrue(exception.getMessage().contains("past"));
    }

    @Test
    void shouldThrowException_WhenInvalidDateRange() {
        // Given
        String customerName = "Jan Nowak";
        CarType carType = CarType.VAN;
        LocalDateTime pickupDate = IN_3_DAYS;
        LocalDateTime returnDate = TOMORROW;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createReservation(customerName, carType, pickupDate, returnDate)
        );

        assertTrue(exception.getMessage().contains("return date") || exception.getMessage().contains("after pickup"));
    }

    @Test
    void shouldThrowException_WhenZeroDuration() {
        // Given
        String customerName = "Jan Nowak";
        CarType carType = CarType.SEDAN;
        LocalDateTime pickupDate = TOMORROW;
        LocalDateTime returnDate = TOMORROW;

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createReservation(customerName, carType, pickupDate, returnDate)
        );

        assertTrue(exception.getMessage().contains("return date") || exception.getMessage().contains("after pickup"));
    }

    @Test
    void shouldThrowException_WhenInsufficientInventory() {
        // Given
        service.createReservation("Customer 1", CarType.SUV, TOMORROW, IN_3_DAYS);

        // When & Then
        InsufficientInventoryException exception = assertThrows(
                InsufficientInventoryException.class,
                () -> service.createReservation("Customer 2", CarType.SUV, TOMORROW, IN_3_DAYS)
        );

        assertTrue(exception.getMessage().contains("not available"));
    }

    @Test
    void shouldAllowOverlap_WhenInventoryAvailable() {
        // Given
        String customer1 = "Customer 1";
        String customer2 = "Customer 2";
        LocalDateTime pickup1 = TOMORROW;
        LocalDateTime return1 = IN_5_DAYS;
        LocalDateTime pickup2 = IN_3_DAYS; // overlaps with first reservation
        LocalDateTime return2 = IN_5_DAYS.plusDays(2);

        // When
        Reservation reservation1 = service.createReservation(customer1, CarType.SEDAN, pickup1, return1);
        Reservation reservation2 = service.createReservation(customer2, CarType.SEDAN, pickup2, return2);

        // Then
        assertNotNull(reservation1);
        assertNotNull(reservation2);
        assertNotEquals(reservation1.id(), reservation2.id());
    }

    @Test
    void shouldCountOverlappingReservations_Correctly() {
        // Given
        service.createReservation("Customer 1", CarType.SEDAN, TOMORROW, IN_3_DAYS);

        // When
        Long available = service.checkAvailability(CarType.SEDAN, TOMORROW, IN_3_DAYS);

        // Then
        assertEquals(1, available);
    }

    @Test
    void shouldShowFullAvailability_WhenNoOverlap() {
        // Given
        service.createReservation("Customer 1", CarType.SEDAN, TOMORROW, IN_3_DAYS);

        // When
        LocalDateTime futurePickup = IN_5_DAYS.plusDays(5);
        LocalDateTime futureReturn = futurePickup.plusDays(3);
        Long available = service.checkAvailability(CarType.SEDAN, futurePickup, futureReturn);

        // Then
        assertEquals(2, available);
    }
}
