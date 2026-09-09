package com.danasea.backend.security.authentication.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.Pattern;

public record RegisterRequest (

    @Email @NotBlank String email,

    @NotBlank 
    @Size(min = 8, max = 100) 
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character")
    String password
){}
