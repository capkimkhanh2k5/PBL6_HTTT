package com.danasea.backend.modules.ai.application.ports;

import com.danasea.backend.modules.ai.application.ports.CustomerSupportRequestStorePort.Request;

public interface CustomerSupportNoticePort {
    void created(Request request);
    void updated(Request request);
}
