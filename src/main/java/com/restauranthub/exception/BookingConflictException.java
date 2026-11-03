package com.restauranthub.exception;

import org.springframework.http.HttpStatus;

public class BookingConflictException extends ApiException {

    public BookingConflictException(String message) {
        super(HttpStatus.CONFLICT, "BOOKING_CONFLICT", message);
    }

    public BookingConflictException(String resourceType, Long resourceId, String interval) {
        super(HttpStatus.CONFLICT, "RESOURCE_ALREADY_RESERVED",
                String.format("The requested %s (ID: %d) is already reserved or held for interval: %s. Please choose an alternative time or resource.",
                        resourceType, resourceId, interval));
    }
}
