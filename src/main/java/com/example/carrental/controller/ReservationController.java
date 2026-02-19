package com.example.carrental.controller;

import com.example.carrental.domain.CarType;
import com.example.carrental.domain.Reservation;
import com.example.carrental.dto.AvailabilityResponse;
import com.example.carrental.service.ReservationService;
import com.example.carrental.dto.CreateReservationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Reservation> create(@Valid @RequestBody CreateReservationRequest request) {
        Reservation reservation = reservationService.createReservation(request.customerName(),
                request.carType(),
                request.pickupDate(),
                request.returnDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> availability(@RequestParam CarType carType,
                                                             @RequestParam LocalDateTime pickupDate,
                                                             @RequestParam LocalDateTime returnDate) {
        Long count = reservationService.checkAvailability(carType, pickupDate, returnDate);
        AvailabilityResponse response = new AvailabilityResponse(count);
        return ResponseEntity.ok(response);
    }
}