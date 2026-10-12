package org.example.authservice.repository;

import org.example.authservice.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Integer> {

    Mono<User> findByEmail(String email);

    Mono<User> findByMobileNumber(String mobileNumber);
}