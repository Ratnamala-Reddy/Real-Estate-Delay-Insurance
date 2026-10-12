package org.example.paymentservice.exception;

public class QuoteNotEligibleForPaymentException
        extends RuntimeException {

    public QuoteNotEligibleForPaymentException(
            String message
    ) {
        super(message);
    }
}
