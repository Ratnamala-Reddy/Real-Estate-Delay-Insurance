package org.example.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Enter a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[0-9]{10}$",
            message = "Mobile number must contain exactly 10 digits"
    )
    private String mobileNumber;

    @NotBlank(message = "Password is required")
    @Size(
            min = 8,
            message = "Password must contain at least 8 characters"
    )
    private String password;
}