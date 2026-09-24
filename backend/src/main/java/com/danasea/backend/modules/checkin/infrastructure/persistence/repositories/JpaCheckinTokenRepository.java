package com.danasea.backend.modules.checkin.infrastructure.persistence.repositories;

import com.danasea.backend.modules.checkin.infrastructure.persistence.entities.CheckinTokenJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("checkinJpaCheckinTokenRepository")
public interface JpaCheckinTokenRepository extends JpaRepository<CheckinTokenJpaEntity, UUID> {

    Optional<CheckinTokenJpaEntity> findByQrTokenHash(String qrTokenHash);

    Optional<CheckinTokenJpaEntity> findBySubOrderId(UUID subOrderId);

    Optional<CheckinTokenJpaEntity> findFirstBySubOrderIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID subOrderId);

    List<CheckinTokenJpaEntity> findAllBySubOrderId(UUID subOrderId);

    boolean existsByQrTokenHash(String qrTokenHash);

    /**
     * Cập nhật điều kiện nguyên tử theo ID ngăn chặn Race Condition (Double Check-in).
     * Trả về số dòng cập nhật (1 = thành công, 0 = vé đã bị dùng hoặc không tìm thấy).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CheckinTokenEntity c " +
           "SET c.usedAt = :usedAt, c.usedByVendorStaffId = :staffId, c.updatedAt = :usedAt " +
           "WHERE c.id = :id AND c.usedAt IS NULL")
    int markAsUsedAtomic(
            @Param("id") UUID id,
            @Param("usedAt") OffsetDateTime usedAt,
            @Param("staffId") UUID staffId
    );

    /**
     * Cập nhật điều kiện nguyên tử theo mã băm qrTokenHash.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CheckinTokenEntity c " +
           "SET c.usedAt = :usedAt, c.usedByVendorStaffId = :staffId, c.updatedAt = :now " +
           "WHERE c.qrTokenHash = :qrTokenHash AND c.usedAt IS NULL")
    int markAsUsedIfUnused(
            @Param("qrTokenHash") String qrTokenHash,
            @Param("staffId") UUID staffId,
            @Param("usedAt") OffsetDateTime usedAt,
            @Param("now") OffsetDateTime now
    );

    /**
     * Khóa dòng bi quan phục vụ kịch bản khóa đồng bộ chặt chẽ.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CheckinTokenEntity c WHERE c.qrTokenHash = :qrTokenHash")
    Optional<CheckinTokenJpaEntity> findByQrTokenHashWithLock(@Param("qrTokenHash") String qrTokenHash);
}
