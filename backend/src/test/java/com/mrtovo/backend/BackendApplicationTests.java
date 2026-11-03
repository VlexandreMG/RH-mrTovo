package com.mrtovo.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BackendApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void apiCandidatExigeUneAuthentification() throws Exception {
		mockMvc.perform(get("/api/candidat/propositions"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void compteCandidatNePeutPasAppelerApiRh() throws Exception {
		mockMvc.perform(get("/api/bloc3/propositions")
						.with(SecurityMockMvcRequestPostProcessors.user("candidat@example.com").roles("CANDIDAT")))
				.andExpect(status().isForbidden());
	}
}
