package org.example.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {

    private String message;

    private String role;

    @JsonIgnore
    private String token;
}