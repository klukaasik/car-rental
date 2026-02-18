package com.example.carrental.exception;

public class InsufficientInventoryException extends RuntimeException {

    public InsufficientInventoryException(ReservationError error) {
        super(error.getMessage());
    }
}
