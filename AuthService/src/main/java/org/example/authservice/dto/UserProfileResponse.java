package org.example.authservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {

    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String role;
    private String status;
}