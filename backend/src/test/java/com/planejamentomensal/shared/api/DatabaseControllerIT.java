package com.planejamentomensal.shared.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.shared.application.Backups;
import com.planejamentomensal.shared.infra.storage.AppPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Own data folder: restoring replaces the database file, which other test contexts keep open
@SpringBootTest(properties = "app.home=${java.io.tmpdir}/pm-database-${random.uuid}")
@AutoConfigureMockMvc
class DatabaseControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private Backups backups;
    @Autowired private AppPaths paths;

    private void horizon(String months) throws Exception {
        mockMvc.perform(authorized(patch("/api/settings")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"projection-horizon-months\":\"" + months + "\"}")).andExpect(status().isOk());
    }

    private void use(String backup) throws Exception {
        mockMvc.perform(authorized(put("/api/database")).contentType(MediaType.APPLICATION_JSON)
                        .content(backup == null ? "{}" : "{\"backup\":\"" + backup + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(backup == null));
    }

    @AfterEach
    void backToLive() throws Exception {
        use(null);
        horizon("12");
    }

    @Test
    @DisplayName("IMP-58, POR-12: a backup opens as a copy to check it, refuses a new backup and the live data comes back intact")
    void viewBackup() throws Exception {
        horizon("5");
        String backup = backups.backup("manual").getFileName().toString();
        horizon("7");

        use(backup);
        mockMvc.perform(authorized(get("/api/database")))
                .andExpect(jsonPath("$.live").value(false))
                .andExpect(jsonPath("$.backup").value(backup));
        mockMvc.perform(authorized(get("/api/settings"))).andExpect(jsonPath("$.projection-horizon-months").value("5"));
        horizon("9");
        mockMvc.perform(authorized(post("/api/backups")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("backup-open"));

        use(null);
        mockMvc.perform(authorized(get("/api/settings"))).andExpect(jsonPath("$.projection-horizon-months").value("7"));
        try (Stream<Path> files = Files.list(paths.temp())) {
            assertThat(files.map(p -> p.getFileName().toString())).noneMatch(name -> name.startsWith("backup-view-"));
        }
        assertThat(backups.list()).anyMatch(p -> p.getFileName().toString().equals(backup));
    }

    @Test
    @DisplayName("IMP-58, POR-14: restoring a backup backs the live data up first and makes the backup the live data")
    void restore() throws Exception {
        horizon("4");
        String backup = backups.backup("manual").getFileName().toString();
        horizon("8");
        use(backup);

        mockMvc.perform(authorized(post("/api/database/restores")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backup\":\"" + backup + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.live").value(true));
        mockMvc.perform(authorized(get("/api/settings"))).andExpect(jsonPath("$.projection-horizon-months").value("4"));
        assertThat(backups.list()).anyMatch(p -> p.getFileName().toString().endsWith("-before-restore.db"));
    }

    @Test
    @DisplayName("IMP-58: an unknown backup name is not found and nothing changes")
    void unknownBackup() throws Exception {
        mockMvc.perform(authorized(put("/api/database")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backup\":\"../planejamento-mensal.db\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(authorized(get("/api/database"))).andExpect(jsonPath("$.live").value(true));
    }
}
