package org.example.applicationservice.Repository;

import org.example.applicationservice.Entity.Application;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ApplicationRepository
        extends ReactiveCrudRepository<Application, String> {

    Flux<Application> findByUserId(Integer userId);

    Mono<Application> findByUserIdAndProjectNameAndUnitNumber(
            Integer userId,
            String projectName,
            String unitNumber
    );
}