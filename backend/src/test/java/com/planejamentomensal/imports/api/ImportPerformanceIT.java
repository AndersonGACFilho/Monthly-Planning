package com.planejamentomensal.imports.api;

import static com.planejamentomensal.member.MemberTestData.memberJson;
import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static com.planejamentomensal.shared.api.JsonBodies.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.imports.infra.itau.Fixtures;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImportPerformanceIT {

    private static final int LINES = 300;
    private static final int FIRST_LINE = 14;

    @Autowired
    private MockMvc mockMvc;

    /**
     * The masked open bill with its purchase lines repeated until the bill has 300 distinct lines.
     */
    private static byte[] bigBill() throws Exception {
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(Fixtures.bytes(Fixtures.OPEN_XLSX)));
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.getSheetAt(0);
            Row model = sheet.getRow(FIRST_LINE + 2);
            double date = model.getCell(1).getNumericCellValue();
            org.apache.poi.ss.usermodel.CellStyle dateStyle = model.getCell(1).getCellStyle();
            String[] tail = {model.getCell(6).getStringCellValue(), model.getCell(7).getStringCellValue(),
                    model.getCell(8).getStringCellValue(), model.getCell(9).getStringCellValue()};
            for (int r = sheet.getLastRowNum(); r >= FIRST_LINE; r--) {
                if (sheet.getRow(r) != null) {
                    sheet.removeRow(sheet.getRow(r));
                }
            }
            for (int m = sheet.getNumMergedRegions() - 1; m >= 0; m--) {
                if (sheet.getMergedRegion(m).getFirstRow() >= FIRST_LINE) {
                    sheet.removeMergedRegion(m);
                }
            }
            for (int i = 0; i < LINES; i++) {
                Row row = sheet.createRow(FIRST_LINE + i);
                Cell day = row.createCell(1);
                day.setCellValue(date - i % 25);
                day.setCellStyle(dateStyle);
                row.createCell(2).setCellValue("Loja Desempenho " + i);
                row.createCell(4).setCellValue(10 + i);
                for (int c = 0; c < tail.length; c++) {
                    row.createCell(6 + c).setCellValue(tail[c]);
                }
            }
            book.write(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("RNF-03: preview and confirmation of a 300-line bill take less than 5 seconds")
    void threeHundredLines() throws Exception {
        var member = mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("PF", "Casa", "MAIN"))).andReturn();
        long memberId = ((Number) read(member, "$.id")).longValue();
        var card = mockMvc.perform(authorized(post("/api/cards")).contentType(MediaType.APPLICATION_JSON).content("""
                {"nickname":"Itaú perf","bank":"ITAU","lastDigits":["4102","4090"],"holderName":"MARIANA S F SILVA",
                 "defaultMemberId":%d,"closingDay":2,"dueDay":9}""".formatted(memberId))).andReturn();
        long cardId = ((Number) read(card, "$.id")).longValue();
        byte[] bill = bigBill();

        long start = System.nanoTime();
        var preview = mockMvc.perform(authorized(multipart("/api/imports")
                        .file(new MockMultipartFile("file", "big.xlsx", "application/octet-stream", bill))
                        .param("cardId", String.valueOf(cardId)).param("billingPeriod", "2026-11")))
                .andExpect(status().isCreated()).andReturn();
        String self = read(preview, "$._links.self.href");
        mockMvc.perform(authorized(get(self))).andExpect(jsonPath("$.lines.length()").value(LINES));
        String confirm = read(preview, "$._links.confirm.href");
        mockMvc.perform(authorized(post(confirm))).andExpect(status().isOk());
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertThat(millis).as("import of %d lines took %d ms", LINES, millis).isLessThan(5_000);
    }
}
