package com.danasea.backend.modules.service.application.usecases;

import java.util.List;

import com.danasea.backend.modules.service.application.dtos.RejectServiceCommand;
import com.danasea.backend.modules.service.application.dtos.ServiceResult;
import com.danasea.backend.modules.service.application.usecases.helpers.ServiceResultMapper;
import com.danasea.backend.modules.service.domain.exceptions.ServiceNotFoundException;
import com.danasea.backend.modules.service.domain.models.Service;
import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.domain.ports.AuditLogPort;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RejectServiceUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ServiceImageRepositoryPort serviceImageRepository;
    private final AuditLogPort auditLogPort;

    public ServiceResult execute(RejectServiceCommand cmd) {
        Service service = serviceRepository.findById(cmd.serviceId())
                .orElseThrow(() -> new ServiceNotFoundException(cmd.serviceId()));

        service.reject(cmd.reason());

        Service saved = serviceRepository.save(service);

        auditLogPort.recordAuditLog(
                cmd.adminUserId(),
                "SERVICE_REJECTED",
                "SERVICE",
                saved.getId(),
                "Rejection reason: " + cmd.reason()
        );

        List<ServiceImage> images = serviceImageRepository.findByServiceId(saved.getId());
        return ServiceResultMapper.toResult(saved, images);
    }
}
