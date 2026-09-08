package com.danasea.backend.modules.account.infrastructure.persistence.entities;

import com.danasea.backend.modules.account.domain.models.Role;

import com.danasea.backend.shared.core.infrastructure.persistence.entities.BaseJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
public class UserJpaEntity extends BaseJpaEntity {

    private String email;

    private String phone;

    private String passwordHash;

    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String avatarUrl;

    private Boolean isEmailVerified;

    private Boolean isLocked;

    private String locale;

}
