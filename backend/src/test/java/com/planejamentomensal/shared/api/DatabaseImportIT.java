package com.planejamentomensal.shared.api;

import static com.planejamentomensal.member.MemberTestData.memberJson;
import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.shared.application.Backups;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Imports database files as the app does, each request committing on a data folder of its own: the imported copy is
 * read on other connections and the automatic cleanup runs on the real backups folder.
 */
@SpringBootTest(properties = "app.home=${java.io.tmpdir}/pm-database-import-${random.uuid}")
@AutoConfigureMockMvc
class DatabaseImportIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Backups backups;

    @TempDir
    private Path folder;

    @AfterEach
    void backToLive() throws Exception {
        mockMvc.perform(authorized(put("/api/database")).contentType(MediaType.APPLICATION_JSON).content("{}"));
    }

    private void member(String code) throws Exception {
        mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                .content(memberJson(code, "Conta " + code, "MAIN"))).andExpect(status().isCreated());
    }

    private static MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("file", "planejamento-mensal-2026-11-13.db", "application/vnd.sqlite3", content);
    }

    @Test
    @DisplayName("IMP-72: an exported database is imported as a copy to check, kept out of the cleanup, live data untouched")
    void importsExportedDatabase() throws Exception {
        member("IA");
        byte[] exported = mockMvc.perform(authorized(get("/api/data-export")).param("format", "db"))
                .andReturn().getResponse().getContentAsByteArray();
        member("IB");

        String name = mockMvc.perform(authorized(multipart("/api/database/imports").file(file(exported))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.backup").value(org.hamcrest.Matchers.matchesPattern("planejamento-mensal-.+-imported\\.db")))
                .andReturn().getResponse().getContentAsString().replaceAll(".*\"backup\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(authorized(get("/api/members")))
                .andExpect(jsonPath("$._embedded.members[?(@.code == 'IA')]").exists())
                .andExpect(jsonPath("$._embedded.members[?(@.code == 'IB')]").doesNotExist());

        backToLive();
        mockMvc.perform(authorized(get("/api/members")))
                .andExpect(jsonPath("$._embedded.members[?(@.code == 'IB')]").exists());
        for (int i = 0; i <= Backups.KEPT; i++) {
            backups.backup("manual");
        }
        List<String> names = backups.list().stream().map(p -> p.getFileName().toString()).toList();
        assertThat(names).contains(name);
    }

    @Test
    @DisplayName("IMP-72: a file that is not a database of this app, or cannot be opened, is refused and nothing is kept")
    void refusesOtherFiles() throws Exception {
        int before = backups.list().size();
        mockMvc.perform(authorized(multipart("/api/database/imports").file(file("not a database".getBytes()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("database-file-invalid"));
        Path other = folder.resolve("other.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + other)) {
            connection.createStatement().execute("create table notes (text varchar)");
        }
        mockMvc.perform(authorized(multipart("/api/database/imports").file(file(Files.readAllBytes(other)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("database-file-invalid"));
        Path broken = folder.resolve("broken.db");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + broken)) {
            connection.createStatement().execute("create table flyway_schema_history (installed_rank integer)");
        }
        mockMvc.perform(authorized(multipart("/api/database/imports").file(file(Files.readAllBytes(broken)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("database-file-invalid"));
        assertThat(backups.list()).hasSize(before);
        mockMvc.perform(authorized(get("/api/database"))).andExpect(jsonPath("$.live").value(true));
    }
}
