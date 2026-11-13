package com.planejamentomensal.recurrence.api;

import static com.planejamentomensal.member.MemberTestData.memberJson;
import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static com.planejamentomensal.shared.api.JsonBodies.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Removes rules as the app does, each request committing on a database of its own: a shared test transaction
 * rolls back before the flush that used to fail.
 */
@SpringBootTest(properties = "app.home=${java.io.tmpdir}/pm-rule-deletion-${random.uuid}")
@AutoConfigureMockMvc
class RuleDeletionIT {

    @Autowired
    private MockMvc mockMvc;

    private String rule(String description) throws Exception {
        var member = mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                .content(memberJson(description.substring(0, 2).toUpperCase(), description, "MAIN"))).andReturn();
        long memberId = ((Number) read(member, "$.id")).longValue();
        return mockMvc.perform(authorized(post("/api/recurrence-rules?kind=INCOME")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"%s","memberId":%d,"defaultAmount":900.00,"recurrence":"MONTHLY","day":5,
                                 "validFrom":"2026-10"}
                                """.formatted(description, memberId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);
    }

    private long occurrence(String period, String description) throws Exception {
        var result = mockMvc.perform(authorized(get("/api/occurrences?billingPeriod=" + period + "&kind=INCOME"))).andReturn();
        List<Number> ids = read(result, "$._embedded.occurrences[?(@.description == '" + description + "')].id");
        return ids.getFirst().longValue();
    }

    @Test
    @DisplayName("IMP-63: an income whose months were generated is deleted with its months")
    void deleteWithGeneratedMonths() throws Exception {
        String location = rule("Freela");
        mockMvc.perform(authorized(get("/api/occurrences?billingPeriod=2026-11&kind=INCOME"))).andExpect(status().isOk());
        mockMvc.perform(authorized(delete(location))).andExpect(status().isNoContent());
        mockMvc.perform(authorized(get(location))).andExpect(status().isNotFound());
        mockMvc.perform(authorized(get("/api/occurrences?billingPeriod=2026-11&kind=INCOME")))
                .andExpect(jsonPath("$._embedded.occurrences[?(@.description == 'Freela')]").doesNotExist());
    }

    @Test
    @DisplayName("IMP-63: an income with a received month is ended from a later month, keeping what was received")
    void endWithReceivedMonth() throws Exception {
        String location = rule("Bolsa");
        long october = occurrence("2026-10", "Bolsa");
        mockMvc.perform(authorized(post("/api/occurrences/" + october + "/status")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"done\":true,\"date\":\"2026-10-06\"}")).andExpect(status().isOk());
        mockMvc.perform(authorized(get("/api/occurrences?billingPeriod=2026-12&kind=INCOME"))).andExpect(status().isOk());
        mockMvc.perform(authorized(post(location + "/end")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"billingPeriod\":\"2026-11\"}")).andExpect(status().isNoContent());
        mockMvc.perform(authorized(get(location))).andExpect(jsonPath("$.validUntil").value("2026-10"));
        mockMvc.perform(authorized(get("/api/occurrences?billingPeriod=2026-10&kind=INCOME")))
                .andExpect(jsonPath("$._embedded.occurrences[?(@.description == 'Bolsa')].status").value("DONE"));
        mockMvc.perform(authorized(get("/api/occurrences?billingPeriod=2026-12&kind=INCOME")))
                .andExpect(jsonPath("$._embedded.occurrences[?(@.description == 'Bolsa')]").doesNotExist());
    }
}
