package com.danasea.backend.modules.admin.presentation;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.danasea.backend.modules.admin.application.usecase.GetAuditLogDetailUseCase;
import com.danasea.backend.modules.admin.application.usecase.GetAuditLogsUseCase;
import com.danasea.backend.modules.admin.presentation.dto.AuditLogResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final GetAuditLogsUseCase getAuditLogsUseCase;
    private final GetAuditLogDetailUseCase getAuditLogDetailUseCase;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLogResponse> response = getAuditLogsUseCase.execute(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditLogResponse> getAuditLogDetail(@PathVariable UUID id) {
        AuditLogResponse response = getAuditLogDetailUseCase.execute(id);
        return ResponseEntity.ok(response);
    }
}
