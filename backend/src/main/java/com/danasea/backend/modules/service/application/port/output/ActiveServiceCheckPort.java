package com.danasea.backend.modules.service.application.port.output;

import java.util.UUID;

public interface ActiveServiceCheckPort {
    boolean hasActiveServices(UUID categoryId);
}
