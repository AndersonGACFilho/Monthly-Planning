package com.planejamentomensal.shared.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.hamcrest.Matchers.hasItems;
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
class ProfileControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("API-10: profile lists the custom relations")
    void listsRelations() throws Exception {
        mockMvc.perform(authorized(get("/api/profile")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.relationDescriptionList[*].name",
                        hasItems("rename", "restore-original", "confirm", "undo", "export", "send")));
    }

    @Test
    @DisplayName("API-10: one relation is described at its own URL")
    void describesOneRelation() throws Exception {
        mockMvc.perform(authorized(get("/api/profile/restore-original")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("restore-original"))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api/profile/restore-original"));
    }
}
