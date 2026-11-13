package com.planejamentomensal.shared.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.home=${java.io.tmpdir}/pm-diagnostics-${random.uuid}")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class DiagnosticsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void debugOff() throws Exception {
        mockMvc.perform(authorized(put("/api/diagnostics")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"debugMode\":false}"));
    }

    @Test
    @DisplayName("RNF-11: the debug mode is read and switched, and turns the app logs to DEBUG")
    void switchesDebugMode() throws Exception {
        mockMvc.perform(authorized(get("/api/diagnostics")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debugMode").value(false))
                .andExpect(jsonPath("$.logFolder").value(org.hamcrest.Matchers.endsWith("logs")))
                .andExpect(jsonPath("$._links.client-errors.href").exists());
        mockMvc.perform(authorized(put("/api/diagnostics")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"debugMode\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.debugMode").value(true));
        assertThat(LoggerFactory.getLogger("com.planejamentomensal.any").isDebugEnabled()).isTrue();
        mockMvc.perform(authorized(get("/api"))).andExpect(jsonPath("$._links.diagnostics.href").exists());
    }

    @Test
    @DisplayName("RNF-11: an interface error is written to the log with its request id")
    void logsClientErrors(CapturedOutput output) throws Exception {
        mockMvc.perform(authorized(post("/api/diagnostics/client-errors")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Cannot read properties of undefined","detail":"at OverviewPage (x.js:1)",
                                 "url":"/overview","requestId":"front-7"}
                                """))
                .andExpect(status().isNoContent());
        assertThat(output).contains("Interface error", "Cannot read properties of undefined", "front-7", "/overview");
    }
}
