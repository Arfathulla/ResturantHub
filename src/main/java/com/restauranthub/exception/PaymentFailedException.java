package com.restauranthub.exception;

import org.springframework.http.HttpStatus;

public class PaymentFailedException extends ApiException {

    public PaymentFailedException(String message) {
        super(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", message);
    }

    public PaymentFailedException(String message, Throwable cause) {
        super(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", message, cause);
    }
}
