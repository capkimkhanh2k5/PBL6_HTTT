package com.danasea.backend.modules.account.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        
        @NotBlank 
        @Size(min = 8, max = 100) 
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$", 
                 message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character")
        String newPassword
) {}
