package org.example.authservice.entity;
import org.example.authservice.enums.Role;
import org.example.authservice.enums.UserStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("users")
public class User {
    @Id
    private int Userid;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String password;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
