package org.example.applicationservice.dto;

import lombok.Data;

@Data
public class UserProfileResponse {

    private String userId;

    private String firstName;

    private String lastName;

    private String email;

    private String mobileNumber;

    private String role;

    private String status;

    private String kycStatus;
}