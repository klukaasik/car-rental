package com.example.carrental.repository;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;

import java.util.List;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    List<Reservation> findByCarType(CarType carType);
}
