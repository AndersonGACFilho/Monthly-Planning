package com.planejamentomensal.summary.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HistoryControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;

    private LedgerFixture f;

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "8");
        ledger.addPurchase(null, f.purchase("MERCADO", "120.00", LocalDate.of(2026, 10, 20), BillingPeriod.of(2026, 11), null,
                null, "PURCHASE"));
    }

    @Test
    @DisplayName("RF-10, RF-15: the history lists each month of the range with totals by member and by category")
    void history() throws Exception {
        mockMvc.perform(authorized(get("/api/history?from=2026-10&to=2026-12&member=" + f.main)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(3))
                .andExpect(jsonPath("$.points[1].billingPeriod").value("2026-11"))
                .andExpect(jsonPath("$.points[1].card").value(120.0))
                .andExpect(jsonPath("$.points[1].byMember['%d']".formatted(f.main)).value(120.0))
                .andExpect(jsonPath("$.points[0].expense").value(0.0))
                .andExpect(jsonPath("$._links.self.href").value("/api/history?from=2026-10&to=2026-12&member=" + f.main));
        mockMvc.perform(authorized(get("/api/history"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(12));
        mockMvc.perform(authorized(get("/api/history?from=2026-12&to=2026-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("history-range-invalid"));
        mockMvc.perform(authorized(get("/api"))).andExpect(jsonPath("$._links.history.href").exists());
    }
}
