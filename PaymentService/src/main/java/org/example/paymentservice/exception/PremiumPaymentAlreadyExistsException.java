package org.example.paymentservice.exception;

public class PremiumPaymentAlreadyExistsException
        extends RuntimeException {

    public PremiumPaymentAlreadyExistsException(
            String message
    ) {
        super(message);
    }
}
