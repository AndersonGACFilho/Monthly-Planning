package com.planejamentomensal.shared.infra.backup;

import com.planejamentomensal.shared.application.DataExport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads every application table straight from SQLite for the export (RNF-07). Money columns stay in cents and
 * dates as ISO text, exactly as stored.
 *
 * @since 1.0
 */
@Component
public class JdbcDataExport implements DataExport {

    private final JdbcTemplate jdbc;
    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Creates the export.
     *
     * @param dataSource database
     */
    public JdbcDataExport(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    @Override
    public byte[] json() {
        Map<String, List<Map<String, Object>>> all = new LinkedHashMap<>();
        tables().forEach(table -> all.put(table, rows(table)));
        return json.writerWithDefaultPrettyPrinter().writeValueAsBytes(all);
    }

    @Override
    public byte[] csvZip() {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String table : tables()) {
                zip.putNextEntry(new ZipEntry(table + ".csv"));
                zip.write(csv(table).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<String> tables() {
        return jdbc.queryForList("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' "
                + "AND name <> 'flyway_schema_history' ORDER BY name", String.class);
    }

    private List<Map<String, Object>> rows(String table) {
        return jdbc.queryForList("SELECT * FROM \"" + table.replace("\"", "\"\"") + "\" ORDER BY rowid");
    }

    private String csv(String table) {
        List<String> columns = jdbc.queryForList("SELECT name FROM pragma_table_info(?)", String.class, table);
        StringBuilder out = new StringBuilder("﻿");
        out.append(String.join(",", columns.stream().map(JdbcDataExport::quote).toList())).append("\r\n");
        for (Map<String, Object> row : rows(table)) {
            out.append(String.join(",", columns.stream().map(c -> quote(row.get(c) == null ? "" : String.valueOf(row.get(c)))).toList()))
                    .append("\r\n");
        }
        return out.toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
