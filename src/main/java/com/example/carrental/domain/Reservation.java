package com.example.carrental.domain;

import java.time.LocalDateTime;

public record Reservation(String id, String customerName, CarType carType, LocalDateTime pickupDate, LocalDateTime returnDate) {}
