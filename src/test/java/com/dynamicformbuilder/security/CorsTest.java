package com.dynamicformbuilder.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cors.allowed-origins=http://first.test,http://second.test")
class CorsTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void everyOriginInTheCommaSeparatedListIsAllowed() throws Exception {
		for (String origin : new String[] { "http://first.test", "http://second.test" }) {
			mockMvc.perform(options("/api/anything")
							.header("Origin", origin)
							.header("Access-Control-Request-Method", "POST")
							.header("Access-Control-Request-Headers", "authorization,content-type"))
					.andExpect(status().isOk())
					.andExpect(header().string("Access-Control-Allow-Origin", origin));
		}
	}

	@Test
	void originNotInTheListIsRejected() throws Exception {
		mockMvc.perform(options("/api/anything")
						.header("Origin", "http://evil.test")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden());
	}
}
