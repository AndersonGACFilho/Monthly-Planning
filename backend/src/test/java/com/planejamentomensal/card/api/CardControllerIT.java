package com.planejamentomensal.card.api;

import static com.planejamentomensal.member.MemberTestData.memberJson;
import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static com.planejamentomensal.shared.api.JsonBodies.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.shared.domain.BillingPeriod;
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
class CardControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CardQueries queries;

    private long memberId;

    @BeforeEach
    void member() throws Exception {
        var result = mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                .content(memberJson("AB", "Casa", "MAIN"))).andReturn();
        memberId = ((Number) read(result, "$.id")).longValue();
    }

    private String cardJson(String nickname, String finals, Long primary) {
        return """
                {"nickname":"%s","bank":"ITAU","lastDigits":[%s],"holderName":"mariana s f silva",
                 "primaryCardId":%s,"defaultMemberId":%d,"closingDay":3,"dueDay":10}
                """.formatted(nickname, finals, primary, memberId);
    }

    private String create(String nickname, String finals, Long primary) throws Exception {
        return mockMvc.perform(authorized(post("/api/cards")).contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson(nickname, finals, primary)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);
    }

    private long id(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    @DisplayName("RF-03: card is created with finals, holder, default member and days")
    void createsCard() throws Exception {
        String location = create("Itaú Platinum", "\"4102\",\"4090\"", null);
        mockMvc.perform(authorized(get(location)))
                .andExpect(jsonPath("$.lastDigits[0]").value("4102"))
                .andExpect(jsonPath("$.holderName").value("MARIANA S F SILVA"))
                .andExpect(jsonPath("$.cycles[0].closingDay").value(3))
                .andExpect(jsonPath("$.cycles[0].dueDay").value(10))
                .andExpect(jsonPath("$._links.default-member.href").value("/api/members/" + memberId))
                .andExpect(jsonPath("$._links.cycles.href").exists());
    }

    @Test
    @DisplayName("RF-04: changing days applies from a period on")
    void changesCycle() throws Exception {
        String location = create("Itaú", "\"1111\"", null);
        mockMvc.perform(authorized(post(location + "/cycles")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingDay\":28,\"dueDay\":5,\"validFrom\":\"2027-01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cycles.length()").value(2));
        assertCycle(id(location), BillingPeriod.of(2026, 12), 10);
        assertCycle(id(location), BillingPeriod.of(2027, 2), 5);
    }

    private void assertCycle(long cardId, BillingPeriod period, int dueDay) {
        org.assertj.core.api.Assertions.assertThat(queries.cycleFor(cardId, period).orElseThrow().dueDay()).isEqualTo(dueDay);
    }

    @Test
    @DisplayName("RF-05: additional card points to the primary card and follows its cycle")
    void additionalCard() throws Exception {
        String primary = create("Titular", "\"2222\"", null);
        String additional = create("Adicional", "\"9103\"", id(primary));
        mockMvc.perform(authorized(get(additional)))
                .andExpect(jsonPath("$.primaryCardId").value(id(primary)))
                .andExpect(jsonPath("$.cycles.length()").value(0))
                .andExpect(jsonPath("$._links.primary-card.href").exists());
        assertCycle(id(additional), BillingPeriod.of(2026, 11), 10);
        org.assertj.core.api.Assertions.assertThat(queries.findByFinal("9103").orElseThrow().billingCardId())
                .isEqualTo(id(primary));
        mockMvc.perform(authorized(get(primary))).andExpect(jsonPath("$._links.delete").doesNotExist());
        mockMvc.perform(authorized(post(additional + "/cycles")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingDay\":1,\"dueDay\":8,\"validFrom\":\"2027-01\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("additional-card-cycle"));
    }

    @Test
    @DisplayName("RF-66: a final belongs to one card only")
    void finalsAreUnique() throws Exception {
        create("A", "\"3333\"", null);
        mockMvc.perform(authorized(post("/api/cards")).contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("B", "\"3333\"", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("card-final-taken"));
    }

    @Test
    @DisplayName("RF-02: default member must exist and becomes undeletable")
    void defaultMember() throws Exception {
        create("C", "\"4444\"", null);
        mockMvc.perform(authorized(get("/api/members/" + memberId))).andExpect(jsonPath("$._links.delete").doesNotExist());
        mockMvc.perform(authorized(post("/api/cards")).contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("D", "\"5555\"", null).replace("\"defaultMemberId\":" + memberId, "\"defaultMemberId\":999")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("card-member-unknown"));
    }

    @Test
    @DisplayName("RF-03: invalid final is a 400 and unused card can be deleted")
    void validationAndDelete() throws Exception {
        mockMvc.perform(authorized(post("/api/cards")).contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("E", "\"41\"", null)))
                .andExpect(status().isBadRequest());
        String location = create("F", "\"6666\"", null);
        mockMvc.perform(authorized(delete(location))).andExpect(status().isNoContent());
    }
}
