package com.danasea.backend.shared.core.infrastructure.persistence.entities;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import com.danasea.backend.shared.core.infrastructure.persistence.generators.AssignedOrRandomUuid;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseJpaEntity implements Persistable<UUID> {

    @Id
    @AssignedOrRandomUuid
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }

    public void markNew() {
        this.isNew = true;
    }

    public void markNotNew() {
        this.isNew = false;
    }

    @PostLoad
    @PostPersist
    void postLoadOrPersist() {
        this.isNew = false;
    }
}
