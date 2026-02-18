package com.example.carrental.repository;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class InMemoryReservationRepository implements ReservationRepository {
    
    private final List<Reservation> reservations = new ArrayList<>();
    
    @Override
    public Reservation save(Reservation reservation) {
        reservations.add(reservation);
        return reservation;
    }
    
    @Override
    public List<Reservation> findByCarType(CarType carType) {
        return reservations.stream()
                .filter(r -> r.carType() == carType)
                .toList();
    }
}
