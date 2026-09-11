package com.danasea.backend;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest()
@ActiveProfiles("test")
public class DdlValidateTest {
    @MockitoBean
    private io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager proxyManager;

    @Test
    void contextLoadsAndValidatesDdl() {

    }
}
