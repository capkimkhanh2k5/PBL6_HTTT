package com.danasea.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	@MockitoBean
	private io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager<byte[]> proxyManager;

	@Autowired
	private RequestMappingHandlerMapping requestMappingHandlerMapping;

	@Test
	void contextLoads() {
	}

	@Test
	void allApplicationEndpointsAreDiscoverable() {
		Set<String> endpoints = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
				.filter(entry -> entry.getValue().getBeanType().getPackageName().startsWith("com.danasea.backend"))
				.flatMap(entry -> entry.getKey().getPatternValues().stream()
						.flatMap(pattern -> entry.getKey().getMethodsCondition().getMethods().stream()
								.map(method -> method.name() + " " + pattern)))
				.collect(Collectors.toSet());

		assertEquals(105, endpoints.size(),
				() -> "Backend API inventory changed; discovered " + endpoints.size() + " endpoints");
	}

}
