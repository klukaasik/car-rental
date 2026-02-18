package com.example.carrental.service;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.exception.InsufficientInventoryException;
import com.example.carrental.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReservationService {
    
    private final ReservationRepository repository;
    private final Map<CarType, Long> inventory;

    public ReservationService(ReservationRepository repository, Map<CarType, Long> inventory) {
        this.repository = repository;
        this.inventory = inventory;
    }

    public Reservation createReservation(String customerName, CarType carType, LocalDateTime pickupDate, LocalDateTime returnDate) {
        validateDates(pickupDate, returnDate);

        Long capacity = inventory.getOrDefault(carType, 0L);
        List<Reservation> existingReservations = repository.findByCarType(carType);
        Long overlappingCount = existingReservations.stream()
                                                    .filter(existing -> existing.pickupDate().isBefore(returnDate) &&
                                                    pickupDate.isBefore(existing.returnDate()))
                                                    .count();
        if (overlappingCount >= capacity) {
            throw new InsufficientInventoryException("Car not available");
        }
        Reservation reservation = new Reservation(
                UUID.randomUUID().toString(),
                customerName,
                carType,
                pickupDate,
                returnDate
        );
        repository.save(reservation);
        return reservation;
    }

    public Long checkAvailability(CarType carType, LocalDateTime pickupDate, LocalDateTime returnDate) {
        Long capacity = inventory.getOrDefault(carType, 0L);
        List<Reservation> existingReservations = repository.findByCarType(carType);
        Long overlappingCount = existingReservations.stream()
                                                    .filter(existing -> existing.pickupDate().isBefore(returnDate) &&
                                                    pickupDate.isBefore(existing.returnDate()))
                                                    .count();
        return Math.max(0, capacity - overlappingCount);
    }

    private void validateDates(LocalDateTime pickupDate, LocalDateTime returnDate) {
        if (!returnDate.isAfter(pickupDate)) {
            throw new IllegalArgumentException("Return date must be after pickup date");
        }
        if (pickupDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Pickup date cannot be in the past");
        }
    }
}
