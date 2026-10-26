package com.planejamentomensal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationStartIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("IMP-02: application starts and reports health UP")
    void applicationStartsAndReportsHealthUp() throws Exception {
        // Given the application context started
        // When the health endpoint is called
        var result = mockMvc.perform(get("/actuator/health"));
        // Then it reports UP
        result.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
