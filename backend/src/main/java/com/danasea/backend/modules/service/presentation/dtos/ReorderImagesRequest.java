package com.danasea.backend.modules.service.presentation.dtos;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderImagesRequest(
        @NotEmpty(message = "{validation.image_ids.not_empty}")
        List<UUID> imageIds
) {}
