package com.planejamentomensal.shared.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RootControllerIT.Contributors.class)
class RootControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class Contributors {

        @Bean
        RootLinkContributor testResources() {
            return () -> Link.of("/api/test-resources", "test-resources");
        }
    }

    @Test
    @DisplayName("API-07: entry point lists self, profile and every contributed top-level resource")
    void entryPointListsResources() throws Exception {
        // Given a module contributed its top-level link
        // When the entry point is read
        var result = mockMvc.perform(authorized(get("/api")).accept(MediaTypes.HAL_JSON));
        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._links.self.href").value("http://localhost/api"))
                .andExpect(jsonPath("$._links.profile.href").value("http://localhost/api/profile"))
                .andExpect(jsonPath("$._links.test-resources.href").value("/api/test-resources"));
    }
}
