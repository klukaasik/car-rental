package com.example.carrental.dto;

import com.example.carrental.domain.CarType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateReservationRequest(

        @NotBlank(message = "Customer name cannot be blank")
        String customerName,

        @NotNull(message = "Car type cannot be null")
        CarType carType,

        @NotNull(message = "Pickup date cannot be null")
        LocalDateTime pickupDate,

        @NotNull(message = "Return date cannot be null")
        LocalDateTime returnDate
) {}