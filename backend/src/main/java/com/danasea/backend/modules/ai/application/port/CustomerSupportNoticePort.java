package com.danasea.backend.modules.ai.application.port;

import com.danasea.backend.modules.ai.application.port.CustomerSupportRequestStorePort.Request;

public interface CustomerSupportNoticePort {
    void created(Request request);
    void updated(Request request);
}
