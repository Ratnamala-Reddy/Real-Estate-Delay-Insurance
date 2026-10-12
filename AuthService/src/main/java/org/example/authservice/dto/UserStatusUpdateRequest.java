package org.example.authservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.authservice.enums.UserStatus;

@Data
public class UserStatusUpdateRequest {

    @NotNull(message = "User status is required")
    private UserStatus status;

    private String reason;
}