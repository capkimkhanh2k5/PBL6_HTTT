package com.danasea.backend;

import org.junit.jupiter.api.Test;
import com.danasea.backend.security.authorization.BaseSecurityIntegrationTest;

public class FullContextLoadTest extends BaseSecurityIntegrationTest {

    @Test
    void contextLoadsWithoutBeanConflicts() {
        System.out.println("FULL CONTEXT LOADED SUCCESSFULLY WITH TESTCONTAINERS!");
    }
}
