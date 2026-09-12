package com.danasea.backend.modules.service.application.dtos;

import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentlyViewedResult {
    private UUID id;
    private UUID serviceId;
    private String serviceName;
    private String primaryImageUrl;
    private LocalDateTime viewedAt;
}
