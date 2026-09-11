package com.danasea.backend.modules.service.application.usecases;

import java.util.List;

import com.danasea.backend.modules.service.application.dto.ServiceResult;
import com.danasea.backend.modules.service.application.usecases.helpers.ServiceResultMapper;
import com.danasea.backend.modules.service.domain.models.ServiceImage;
import com.danasea.backend.modules.service.domain.models.ServiceStatus;
import com.danasea.backend.modules.service.domain.ports.ServiceImageRepositoryPort;
import com.danasea.backend.modules.service.domain.ports.ServiceRepositoryPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetAdminServicesUseCase {

    private final ServiceRepositoryPort serviceRepository;
    private final ServiceImageRepositoryPort serviceImageRepository;

    public List<ServiceResult> execute(ServiceStatus status) {
        var services = (status != null)
                ? serviceRepository.findByStatus(status)
                : serviceRepository.findAll();

        return services.stream()
                .map(svc -> {
                    List<ServiceImage> images = serviceImageRepository.findByServiceId(svc.getId());
                    return ServiceResultMapper.toResult(svc, images);
                })
                .toList();
    }
}
