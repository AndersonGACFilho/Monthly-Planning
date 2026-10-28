package com.planejamentomensal.shared.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SettingsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IMP-10: settings start with their defaults")
    void defaults() throws Exception {
        mockMvc.perform(authorized(get("/api/settings")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alert-warning-percent").value("80"))
                .andExpect(jsonPath("$.alert-danger-percent").value("100"))
                .andExpect(jsonPath("$.auto-validate").value("false"))
                .andExpect(jsonPath("$.theme").value(""))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("IMP-10: changed settings are kept, others keep their value")
    void changesAreStored() throws Exception {
        mockMvc.perform(authorized(patch("/api/settings")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alert-warning-percent\":\"70\",\"language\":\"en-US\",\"theme\":\"dark\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.alert-warning-percent").value("70"))
                .andExpect(jsonPath("$.language").value("en-US"));
        mockMvc.perform(authorized(get("/api/settings")))
                .andExpect(jsonPath("$.alert-warning-percent").value("70"))
                .andExpect(jsonPath("$.alert-danger-percent").value("100"));
    }

    @Test
    @DisplayName("IMP-10, UI-08: unknown key or invalid value is a 409 and changes nothing")
    void invalidChangesAreRejected() throws Exception {
        mockMvc.perform(authorized(patch("/api/settings")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alert-warning-percent\":\"60\",\"auto-validate\":\"maybe\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("invalid-setting"));
        mockMvc.perform(authorized(patch("/api/settings")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nope\":\"1\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(authorized(patch("/api/settings")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"sepia\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(authorized(get("/api/settings")))
                .andExpect(jsonPath("$.alert-warning-percent").value("80"));
    }
}
