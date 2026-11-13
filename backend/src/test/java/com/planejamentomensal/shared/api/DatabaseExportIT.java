package com.planejamentomensal.shared.api;

import static com.planejamentomensal.member.MemberTestData.memberJson;
import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exports the database as the app does: the member is committed before the copy, which is read on another
 * connection, so the test runs without a shared transaction on a data folder of its own.
 */
@SpringBootTest(properties = "app.home=${java.io.tmpdir}/pm-database-export-${random.uuid}")
@AutoConfigureMockMvc
class DatabaseExportIT {

    @Autowired
    private MockMvc mockMvc;

    @TempDir
    private Path folder;

    @Test
    @DisplayName("IMP-69: the whole database is exported as one dated SQLite file that opens with the same data")
    void exportsDatabaseFile() throws Exception {
        mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("DB", "Banco", "MAIN"))).andExpect(status().isCreated());
        byte[] file = mockMvc.perform(authorized(get("/api/data-export")).param("format", "db"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.sqlite3"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.matchesPattern(".*filename=\"planejamento-mensal-\\d{4}-\\d{2}-\\d{2}\\.db\".*")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(file, 0, 15, StandardCharsets.US_ASCII)).isEqualTo("SQLite format 3");
        Path copy = Files.write(folder.resolve("exported.db"), file);
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + copy);
                ResultSet rows = connection.createStatement().executeQuery("select name from member where code = 'DB'")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("Banco");
        }
    }

    @Test
    @DisplayName("IMP-69: an unknown export format is a 400 problem")
    void unknownFormat() throws Exception {
        mockMvc.perform(authorized(get("/api/data-export")).param("format", "xml"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("export-format-invalid"));
    }
}
