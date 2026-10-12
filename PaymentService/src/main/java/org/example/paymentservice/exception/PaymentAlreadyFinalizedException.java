package org.example.paymentservice.exception;

public class PaymentAlreadyFinalizedException
        extends RuntimeException {

    public PaymentAlreadyFinalizedException(
            String message
    ) {
        super(message);
    }
}
