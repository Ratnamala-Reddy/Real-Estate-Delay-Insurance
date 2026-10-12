package org.example.paymentservice.repository;

import org.example.paymentservice.entity.CompensationPayment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CompensationPaymentRepository extends ReactiveCrudRepository<CompensationPayment, String> {
}