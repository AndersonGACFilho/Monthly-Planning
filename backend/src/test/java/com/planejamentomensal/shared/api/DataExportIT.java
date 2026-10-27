package com.planejamentomensal.shared.api;

import static com.planejamentomensal.member.MemberTestData.memberJson;
import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
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
class DataExportIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("RNF-07: every table can be exported as JSON or as CSV files in a ZIP")
    void exports() throws Exception {
        mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("EX", "Exportada", "MAIN"))).andExpect(status().isCreated());
        mockMvc.perform(authorized(get("/api/data-export")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member[?(@.code == 'EX')].name").value("Exportada"))
                .andExpect(jsonPath("$.bill_entry").isArray())
                .andExpect(jsonPath("$.flyway_schema_history").doesNotExist());
        byte[] zip = mockMvc.perform(authorized(get("/api/data-export")).param("format", "csv"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        Map<String, String> files = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                files.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertThat(files).containsKeys("member.csv", "card.csv", "statement_delivery.csv");
        assertThat(files.get("member.csv")).contains("\"code\"").contains("\"EX\"");
    }

    @Test
    @DisplayName("POR-12: Backup now from the API writes into data/backups and lists the copies")
    void backupNow() throws Exception {
        mockMvc.perform(authorized(post("/api/backups")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.file").value(org.hamcrest.Matchers.containsString("backups")));
        mockMvc.perform(authorized(get("/api/backups"))).andExpect(jsonPath("$[0]").exists());
    }
}
