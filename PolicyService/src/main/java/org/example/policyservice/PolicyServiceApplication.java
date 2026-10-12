package org.example.policyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class PolicyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PolicyServiceApplication.class, args);
    }

}
