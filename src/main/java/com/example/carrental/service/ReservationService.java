package com.example.carrental.service;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.exception.InsufficientInventoryException;
import com.example.carrental.exception.ReservationError;
import com.example.carrental.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReservationService {
    
    private final ReservationRepository reservationRepository;
    private final Map<CarType, Long> inventory;

    public ReservationService(ReservationRepository reservationRepository, Map<CarType, Long> inventory) {
        this.reservationRepository = reservationRepository;
        this.inventory = inventory;
    }

    public Reservation createReservation(String customerName, CarType carType, LocalDateTime pickupDate, LocalDateTime returnDate) {
        validateDates(pickupDate, returnDate);
        Long capacity = inventory.getOrDefault(carType, 0L);
        Long overlappingCount = countOverlappingReservations(carType, pickupDate, returnDate);

        if (overlappingCount >= capacity) {
            throw new InsufficientInventoryException(ReservationError.CAR_NOT_AVAILABLE);
        }
        Reservation reservation = new Reservation(
                UUID.randomUUID().toString(),
                customerName,
                carType,
                pickupDate,
                returnDate
        );
        reservationRepository.save(reservation);
        return reservation;
    }

    public Long checkAvailability(CarType carType, LocalDateTime pickupDate, LocalDateTime returnDate) {
        Long capacity = inventory.getOrDefault(carType, 0L);
        Long overlappingCount = countOverlappingReservations(carType, pickupDate, returnDate);
        return Math.max(0, capacity - overlappingCount);
    }

    private void validateDates(LocalDateTime pickupDate, LocalDateTime returnDate) {
        if (!returnDate.isAfter(pickupDate)) {
            throw new IllegalArgumentException(ReservationError.RETURN_DATE_MUST_BE_AFTER_PICKUP.getMessage());
        }
        if (pickupDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(ReservationError.PICKUP_DATE_CANNOT_BE_IN_THE_PAST.getMessage());
        }
//        if (pickupDate.plusHours(1L).isAfter(returnDate)) {
//            throw new IllegalArgumentException("The minimum reservation time is one hour");
//        }
    }

    private Long countOverlappingReservations(CarType carType, LocalDateTime pickupDate, LocalDateTime returnDate) {
        List<Reservation> existingReservations = reservationRepository.findByCarType(carType);
        return existingReservations.stream()
                .filter(existing -> existing.pickupDate().isBefore(returnDate) && pickupDate.isBefore(existing.returnDate()))
                .count();
    }
}
