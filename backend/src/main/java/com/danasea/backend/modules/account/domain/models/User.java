package com.danasea.backend.modules.account.domain.models;

import com.danasea.backend.shared.core.domain.models.BaseDomainModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseDomainModel {
    private String email;
    private String phone;
    private String passwordHash;
    private String fullName;
    private Role role;
    private String avatarUrl;
    private Boolean isEmailVerified;
    private Boolean isLocked;
    private String locale;
}
