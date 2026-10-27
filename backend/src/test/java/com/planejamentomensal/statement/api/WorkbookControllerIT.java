package com.planejamentomensal.statement.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkbookControllerIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("XLS-17, XLS-01: the summary links to the spreadsheet, which downloads with the period file name")
    void download() throws Exception {
        mockMvc.perform(authorized(get("/api/summaries/2026-11")))
                .andExpect(jsonPath("$._links.export-xlsx.href").value("/api/summaries/2026-11/workbook"));
        mockMvc.perform(authorized(get("/api/summaries/2026-11/workbook-options")))
                .andExpect(jsonPath("$.defaultMode").value(Matchers.oneOf("EXCEL_365", "COMPATIBLE")))
                .andExpect(jsonPath("$._links.export-xlsx.href").exists());
        byte[] body = mockMvc.perform(authorized(get("/api/summaries/2026-11/workbook")).param("mode", "COMPATIBLE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("NOVEMBRO")))
                .andExpect(header().string("X-Formula-Mode", "COMPATIBLE"))
                .andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook book = new XSSFWorkbook(new java.io.ByteArrayInputStream(body))) {
            assertThat(book.getSheet("Resumo")).isNotNull();
        }
    }

    @Test
    @DisplayName("XLS-14: a model without the named sheets is refused; the current model downloads and can be reset")
    void model() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook other = new XSSFWorkbook()) {
            other.createSheet("Resumo");
            other.write(out);
        }
        mockMvc.perform(authorized(multipart("/api/workbook-model").file(new MockMultipartFile("file", "x.xlsx", null, out.toByteArray()))
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        })))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("workbook-model-invalid"))
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("CARTÕES")));
        byte[] current = mockMvc.perform(authorized(get("/api/workbook-model"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        mockMvc.perform(authorized(multipart("/api/workbook-model").file(new MockMultipartFile("file", "m.xlsx", null, current))
                        .with(r -> {
                            r.setMethod("PUT");
                            return r;
                        })))
                .andExpect(status().isNoContent());
        mockMvc.perform(authorized(delete("/api/workbook-model"))).andExpect(status().isNoContent());
    }
}
