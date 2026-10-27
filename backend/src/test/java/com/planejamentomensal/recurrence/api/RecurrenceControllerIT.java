package com.planejamentomensal.recurrence.api;

import static com.planejamentomensal.member.MemberTestData.memberJson;
import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static com.planejamentomensal.shared.api.JsonBodies.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class RecurrenceControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private long memberId;

    @BeforeEach
    void member() throws Exception {
        var result = mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("RC", "Casa", "MAIN"))).andReturn();
        memberId = ((Number) read(result, "$.id")).longValue();
    }

    private String rule(String kind, String description, String recurrence, String amount, String extra) throws Exception {
        return mockMvc.perform(authorized(post("/api/recurrence-rules?kind=" + kind)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"%s","memberId":%d,"defaultAmount":%s,"recurrence":"%s","day":5,
                                 "validFrom":"2026-10"%s}
                                """.formatted(description, memberId, amount, recurrence, extra)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);
    }

    private long firstOccurrence(String period, String kind) throws Exception {
        var result = mockMvc.perform(authorized(get("/api/occurrences?billingPeriod=" + period + "&kind=" + kind))).andReturn();
        return ((Number) read(result, "$._embedded.occurrences[0].id")).longValue();
    }

    @Test
    @DisplayName("RF-21, RF-22: an income generates its period, which can change its amount alone")
    void incomeFlow() throws Exception {
        String location = rule("INCOME", "Salário", "MONTHLY", "5000.00", ",\"incomeType\":\"SALARY\"");
        mockMvc.perform(authorized(get(location)))
                .andExpect(jsonPath("$.kind").value("INCOME"))
                .andExpect(jsonPath("$.incomeType").value("SALARY"))
                .andExpect(jsonPath("$._links.inactivate").exists())
                .andExpect(jsonPath("$._links.delete").exists());
        mockMvc.perform(authorized(get("/api/occurrences?billingPeriod=2026-11&kind=INCOME")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.occurrences[0].description").value("Salário"))
                .andExpect(jsonPath("$._embedded.occurrences[0].amount").value(5000.0))
                .andExpect(jsonPath("$._embedded.occurrences[0].status").value("EXPECTED"))
                .andExpect(jsonPath("$._embedded.occurrences[0]._links.mark-received").exists())
                .andExpect(jsonPath("$._embedded.occurrences[0]._links.rule").exists());
        long id = firstOccurrence("2026-11", "INCOME");
        mockMvc.perform(authorized(put("/api/occurrences/" + id + "/amount")).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.IF_MATCH, "\"0\"").content("{\"amount\":4800.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(4800.0))
                .andExpect(jsonPath("$.defaultAmount").value(5000.0))
                .andExpect(jsonPath("$.amountOverridden").value(true));
        mockMvc.perform(authorized(get(location)))
                .andExpect(jsonPath("$.defaultAmount").value(5000.0));
    }

    @Test
    @DisplayName("RF-24: an income is marked received with a date and back to expected; history blocks deletion")
    void received() throws Exception {
        String location = rule("INCOME", "Freela", "MONTHLY", "900.00", "");
        long id = firstOccurrence("2026-10", "INCOME");
        mockMvc.perform(authorized(post("/api/occurrences/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true,\"date\":\"2026-10-06\"}"))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.effectiveDate").value("2026-10-06"))
                .andExpect(jsonPath("$._links.mark-expected").exists());
        mockMvc.perform(authorized(get(location))).andExpect(jsonPath("$._links.delete").doesNotExist());
        mockMvc.perform(authorized(delete(location))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("rule-has-history"));
        mockMvc.perform(authorized(patch(location)).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$._links.activate").exists());
        mockMvc.perform(authorized(post("/api/occurrences/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":false}"))
                .andExpect(jsonPath("$.status").value("EXPECTED"))
                .andExpect(jsonPath("$.effectiveDate").doesNotExist());
        mockMvc.perform(authorized(delete(location))).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("RF-23: a one-off income appears only in its period")
    void oneOff() throws Exception {
        rule("INCOME", "Venda bicicleta", "ONCE", "700.00", ",\"incomeType\":\"OTHER\"");
        mockMvc.perform(authorized(get("/api/occurrences?billingPeriod=2026-10&kind=INCOME")))
                .andExpect(jsonPath("$._embedded.occurrences.length()").value(1));
        mockMvc.perform(authorized(get("/api/occurrences?billingPeriod=2026-11&kind=INCOME")))
                .andExpect(jsonPath("$._embedded.occurrences").doesNotExist());
    }

    @Test
    @DisplayName("RF-30, RF-32: a fixed expense keeps category and payment method and is marked paid")
    void fixedExpense() throws Exception {
        rule("FIXED_EXPENSE", "Aluguel", "MONTHLY", "1800.00", ",\"paymentMethod\":\"BOLETO\"");
        mockMvc.perform(authorized(get("/api/recurrence-rules?kind=FIXED_EXPENSE")))
                .andExpect(jsonPath("$._embedded.recurrenceRules[0].paymentMethod").value("BOLETO"))
                .andExpect(jsonPath("$._embedded.recurrenceRules[0].incomeType").doesNotExist());
        long id = firstOccurrence("2026-10", "FIXED_EXPENSE");
        mockMvc.perform(authorized(get("/api/occurrences/" + id)))
                .andExpect(jsonPath("$._links.mark-paid").exists());
        mockMvc.perform(authorized(post("/api/occurrences/" + id + "/status")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true,\"date\":\"2026-10-10\"}"))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @DisplayName("RF-21: invalid rules are rejected with a localized problem")
    void invalid() throws Exception {
        mockMvc.perform(authorized(post("/api/recurrence-rules?kind=INCOME")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"x","memberId":%d,"defaultAmount":1,"recurrence":"MONTHLY","day":5,
                                 "validFrom":"2026-10","validUntil":"2026-09"}
                                """.formatted(memberId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("rule-validity-invalid"));
        mockMvc.perform(authorized(post("/api/recurrence-rules?kind=INCOME")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"x","memberId":999999,"defaultAmount":1,"recurrence":"MONTHLY","day":5,
                                 "validFrom":"2026-10"}
                                """))
                .andExpect(jsonPath("$.code").value("card-member-unknown"));
    }
}
