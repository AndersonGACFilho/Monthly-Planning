package com.planejamentomensal.shared.api.web;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SessionTokenFilterIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IMP-04: API call without session token is rejected with a problem")
    void rejectsMissingToken() throws Exception {
        // Given / When / Then
        mockMvc.perform(get("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("IMP-04: API call with a wrong session token is rejected")
    void rejectsWrongToken() throws Exception {
        mockMvc.perform(get("/api").header(SessionTokenFilter.HEADER, "guess"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("IMP-04: API call with the session token passes")
    void acceptsToken() throws Exception {
        mockMvc.perform(authorized(get("/api"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("POR-08: health stays reachable without token so the shell can wait for startup")
    void healthNeedsNoToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("IMP-04: CORS preflight from the Tauri window is allowed without token")
    void preflightFromTauriOrigin() throws Exception {
        mockMvc.perform(options("/api")
                        .header("Origin", "tauri://localhost")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", SessionTokenFilter.HEADER))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "tauri://localhost"));
    }

    @Test
    @DisplayName("IMP-04: CORS preflight from another origin is refused")
    void preflightFromOtherOrigin() throws Exception {
        mockMvc.perform(options("/api")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
