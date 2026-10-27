package com.planejamentomensal.imports.api;

import static com.planejamentomensal.member.MemberTestData.memberJson;
import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static com.planejamentomensal.shared.api.JsonBodies.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.imports.infra.itau.Fixtures;
import org.junit.jupiter.api.BeforeEach;
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
class ImportControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private long cardId;
    private long memberId;

    @BeforeEach
    void card() throws Exception {
        var member = mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("AB", "Casa", "MAIN"))).andReturn();
        memberId = ((Number) read(member, "$.id")).longValue();
        var card = mockMvc.perform(authorized(post("/api/cards")).contentType(MediaType.APPLICATION_JSON).content("""
                {"nickname":"Itaú","bank":"ITAU","lastDigits":["4102","4090"],"holderName":"MARIANA S F SILVA",
                 "defaultMemberId":%d,"closingDay":2,"dueDay":9}""".formatted(memberId))).andReturn();
        cardId = ((Number) read(card, "$.id")).longValue();
    }

    private String upload(String fixture, String period) throws Exception {
        var file = new MockMultipartFile("file", fixture, "application/octet-stream", Fixtures.bytes(fixture));
        var result = mockMvc.perform(authorized(multipart("/api/imports").file(file)
                        .param("cardId", String.valueOf(cardId)).param("billingPeriod", period)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PREVIEW"))
                .andExpect(jsonPath("$._links.confirm.href").exists())
                .andExpect(jsonPath("$._links.cancel.href").exists())
                .andExpect(jsonPath("$._links.undo").doesNotExist())
                .andReturn();
        return read(result, "$._links.self.href");
    }

    @Test
    @DisplayName("RF-60, RF-68, API-09: upload returns the preview with lines and preview actions")
    void preview() throws Exception {
        String self = upload(Fixtures.CLOSED_PDF, "2026-10");
        mockMvc.perform(authorized(get(self)))
                .andExpect(jsonPath("$.newCount").value(79))
                .andExpect(jsonPath("$.totalsMatch").value(true))
                .andExpect(jsonPath("$.totalDifference").value(0))
                .andExpect(jsonPath("$.lines.length()").value(95))
                .andExpect(jsonPath("$.lines[0]._links.edit.href").exists())
                .andExpect(jsonPath("$.lines[0].installment").value("12/12"));
    }

    @Test
    @DisplayName("RF-69, RF-72, API-09: lines can be edited, the preview confirmed and the batch undone")
    void confirmAndUndo() throws Exception {
        String self = upload(Fixtures.CLOSED_PDF, "2026-10");
        var preview = mockMvc.perform(authorized(get(self))).andReturn();
        String edit = read(preview, "$.lines[0]._links.edit.href");
        mockMvc.perform(authorized(patch(edit)).contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"IGNORE\"}"))
                .andExpect(jsonPath("$.lines[0].decision").value("IGNORE"));
        String confirm = read(preview, "$._links.confirm.href");
        var confirmed = mockMvc.perform(authorized(post(confirm)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$._links.confirm").doesNotExist())
                .andExpect(jsonPath("$._links.undo.href").exists())
                .andReturn();
        mockMvc.perform(authorized(get("/api/review-queue").param("batch", self.substring(self.lastIndexOf('/') + 1))))
                .andExpect(jsonPath("$._embedded.items.length()").value(78))
                .andExpect(jsonPath("$._links.validate-selected.href").exists())
                .andExpect(jsonPath("$._embedded.items[0]._links.validate.href").exists());
        mockMvc.perform(authorized(post((String) read(confirmed, "$._links.undo.href"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDONE"));
        mockMvc.perform(authorized(post(confirm))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("import-not-in-preview"));
    }

    @Test
    @DisplayName("RF-61: wrong file types are refused with a problem and a preview can be cancelled")
    void rejectsAndCancels() throws Exception {
        var csv = new MockMultipartFile("file", "extrato.csv", "text/csv", "a,b".getBytes());
        mockMvc.perform(authorized(multipart("/api/imports").file(csv).param("cardId", String.valueOf(cardId))
                        .param("billingPeriod", "2026-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("unsupported-bill-format"));
        String self = upload(Fixtures.OPEN_XLSX, "2026-11");
        mockMvc.perform(authorized(delete(self))).andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(authorized(get("/api/imports"))).andExpect(jsonPath("$._embedded.imports.length()").value(1));
    }

    @Test
    @DisplayName("RF-76, RF-77, RF-79: review queue validates one item and selected items over the API")
    void reviewQueueApi() throws Exception {
        String self = upload(Fixtures.CLOSED_PDF, "2026-10");
        mockMvc.perform(authorized(post(self + "/confirmation"))).andExpect(status().isOk());
        var category = mockMvc.perform(authorized(post("/api/categories")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Compras\",\"color\":\"#000000\",\"icon\":\"cart\"}")).andReturn();
        long categoryId = ((Number) read(category, "$.id")).longValue();
        var queue = mockMvc.perform(authorized(get("/api/review-queue"))).andReturn();
        String validate = read(queue, "$._embedded.items[0]._links.validate.href");
        mockMvc.perform(authorized(post(validate)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("review-category-required"));
        mockMvc.perform(authorized(post(validate)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":%d}".formatted(categoryId)))
                .andExpect(status().isOk());
        Number second = read(queue, "$._embedded.items[1].purchaseId");
        Number third = read(queue, "$._embedded.items[2].purchaseId");
        mockMvc.perform(authorized(post("/api/review-queue/validations")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purchaseIds\":[%s,%s],\"changes\":{\"categoryId\":%d},\"validate\":true}"
                                .formatted(second, third, categoryId)))
                .andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/review-queue/summary").param("billingPeriod", "2026-10")))
                .andExpect(jsonPath("$.count").value(76));
    }
}
