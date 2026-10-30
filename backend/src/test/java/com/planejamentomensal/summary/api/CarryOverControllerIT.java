package com.planejamentomensal.summary.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryCommands;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.entry.application.ReviewItem;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CarryOverControllerIT {

    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    @Autowired private MockMvc mockMvc;
    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private EntryCommands entries;

    private LedgerFixture f;

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "9");
        BillEntry hers = ledger.addPurchase(null, f.purchase("FARMACIA", "80.00", LocalDate.of(2026, 10, 20), NOV, null, null,
                "PURCHASE"));
        entries.classify(hers.id(), null, false, List.of(new ReviewItem.Share(f.thirdParty, new BigDecimal("100"), null)),
                null, null);
    }

    private String body(long memberId) {
        return "{\"memberIds\":[" + memberId + "]}";
    }

    @Test
    @DisplayName("RF-28: carrying over moves the unpaid amount to the next month, and undoing brings it back")
    void carriesAndUndoes() throws Exception {
        mockMvc.perform(authorized(get("/api/summaries/2026-11")))
                .andExpect(jsonPath("$._links.carry-overs.href").value("/api/summaries/2026-11/carry-overs"))
                .andExpect(jsonPath("$.members[?(@.memberId == %d)].unpaid".formatted(f.thirdParty)).value(80.0));
        mockMvc.perform(authorized(post("/api/summaries/2026-11/carry-overs")).contentType(MediaType.APPLICATION_JSON)
                        .content(body(f.thirdParty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].toPeriod").value("2026-12"))
                .andExpect(jsonPath("$[0].amount").value(80.0));
        mockMvc.perform(authorized(get("/api/summaries/2026-12/members")))
                .andExpect(jsonPath("$[?(@.memberId == %d)].carriedIn".formatted(f.thirdParty)).value(80.0));
        mockMvc.perform(authorized(delete("/api/summaries/2026-11/carry-overs/" + f.thirdParty)))
                .andExpect(status().isNoContent());
        mockMvc.perform(authorized(get("/api/summaries/2026-12/members")))
                .andExpect(jsonPath("$[?(@.memberId == %d)].carriedIn".formatted(f.thirdParty)).value(0.0));
    }

    @Test
    @DisplayName("RF-28, API-03: a main member, a member who owes nothing and an empty list are refused")
    void refuses() throws Exception {
        mockMvc.perform(authorized(post("/api/summaries/2026-11/carry-overs")).contentType(MediaType.APPLICATION_JSON)
                        .content(body(f.main)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("carry-over-main-member"));
        mockMvc.perform(authorized(post("/api/summaries/2026-10/carry-overs")).contentType(MediaType.APPLICATION_JSON)
                        .content(body(f.thirdParty)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("carry-over-nothing-due"));
        mockMvc.perform(authorized(post("/api/summaries/2026-11/carry-overs")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberIds\":[]}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(authorized(delete("/api/summaries/2026-11/carry-overs/" + f.thirdParty)))
                .andExpect(status().isNotFound());
    }
}
