package com.planejamentomensal.expense.api;

import static com.planejamentomensal.member.MemberTestData.memberJson;
import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static com.planejamentomensal.shared.api.JsonBodies.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VariableExpenseControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private long house;
    private long mother;

    private long member(String code, String kind) throws Exception {
        var result = mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                .content(memberJson(code, code, kind))).andReturn();
        return ((Number) read(result, "$.id")).longValue();
    }

    @BeforeEach
    void members() throws Exception {
        house = member("VH", "MAIN");
        mother = member("VM", "THIRD_PARTY");
    }

    private String expense(String date, String description, long memberId, String amount, String method) throws Exception {
        return mockMvc.perform(authorized(post("/api/variable-expenses")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expenseDate":"%s","description":"%s","memberId":%d,"amount":%s,"paymentMethod":"%s"}
                                """.formatted(date, description, memberId, amount, method)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);
    }

    @Test
    @DisplayName("RF-34, RF-35: expenses are listed newest first by period and member with the filter total")
    void listsWithTotal() throws Exception {
        expense("2026-11-03", "Feira", house, "120.50", "PIX");
        expense("2026-11-10", "Farmácia", mother, "45.00", "DEBIT");
        expense("2026-10-28", "Padaria", house, "15.00", "CASH");
        mockMvc.perform(authorized(get("/api/variable-expenses?billingPeriod=2026-11")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].description").value("Farmácia"))
                .andExpect(jsonPath("$.total").value(165.5))
                .andExpect(jsonPath("$._links.self.href").exists());
        mockMvc.perform(authorized(get("/api/variable-expenses?billingPeriod=2026-11&member=" + house)))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(120.5));
        mockMvc.perform(authorized(get("/api/variable-expenses?billingPeriod=2026-10")))
                .andExpect(jsonPath("$.items[0].billingPeriod").value("2026-10"));
    }

    @Test
    @DisplayName("RF-34: an expense is edited with If-Match and deleted")
    void editAndDelete() throws Exception {
        String location = expense("2026-11-03", "Feira", house, "120.50", "PIX");
        mockMvc.perform(authorized(put(location)).contentType(MediaType.APPLICATION_JSON).header(HttpHeaders.IF_MATCH, "\"0\"")
                        .content("""
                                {"expenseDate":"2026-11-30","billingPeriod":"2026-12","description":"Feira grande",
                                 "memberId":%d,"amount":200,"paymentMethod":"DEBIT"}
                                """.formatted(house)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingPeriod").value("2026-12"))
                .andExpect(jsonPath("$.description").value("Feira grande"))
                .andExpect(jsonPath("$._links.delete").exists());
        mockMvc.perform(authorized(put(location)).contentType(MediaType.APPLICATION_JSON).header(HttpHeaders.IF_MATCH, "\"0\"")
                        .content("""
                                {"expenseDate":"2026-11-30","description":"x","memberId":%d,"amount":1,"paymentMethod":"PIX"}
                                """.formatted(house)))
                .andExpect(status().isPreconditionFailed());
        mockMvc.perform(authorized(delete(location))).andExpect(status().isNoContent());
        mockMvc.perform(authorized(get(location))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("RF-34, RF-11: card payments are refused because the bill already holds them")
    void refusesCard() throws Exception {
        mockMvc.perform(authorized(post("/api/variable-expenses")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expenseDate":"2026-11-03","description":"Loja","memberId":%d,"amount":10,"paymentMethod":"CARD"}
                                """.formatted(house)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("expense-payment-invalid"));
    }

    @Test
    @DisplayName("RF-01: a member with variable expenses cannot be deleted")
    void memberReferenced() throws Exception {
        expense("2026-11-03", "Feira", mother, "10", "PIX");
        mockMvc.perform(authorized(get("/api/members/" + mother))).andExpect(jsonPath("$._links.delete").doesNotExist());
    }
}
