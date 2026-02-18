package com.example.carrental.exception;

public enum ReservationError {

    CAR_NOT_AVAILABLE("Car not available"),
    RETURN_DATE_MUST_BE_AFTER_PICKUP("Return date must be after pickup date"),
    PICKUP_DATE_CANNOT_BE_IN_THE_PAST("Pickup date cannot be in the past");

    private final String message;

    ReservationError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
