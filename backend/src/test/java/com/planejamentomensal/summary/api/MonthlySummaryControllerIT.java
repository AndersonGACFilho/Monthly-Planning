package com.planejamentomensal.summary.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.CardBillRepository;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
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
class MonthlySummaryControllerIT {

    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    @Autowired private MockMvc mockMvc;
    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private CardBillRepository bills;

    private LedgerFixture f;

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "5");
        ledger.addPurchase(null, f.purchase("MERCADO", "120.00", LocalDate.of(2026, 10, 20), NOV, null, null, "PURCHASE"));
    }

    @Test
    @DisplayName("IMP-32, RF-10: the summary has indicators and previous, next, by-member and by-category links")
    void summary() throws Exception {
        mockMvc.perform(authorized(get("/api/summaries/2026-11")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingPeriod").value("2026-11"))
                .andExpect(jsonPath("$.cardTotal").value(120.0))
                .andExpect(jsonPath("$.expense").value(120.0))
                .andExpect(jsonPath("$.alert").value("DANGER"))
                .andExpect(jsonPath("$.committedNext.length()").value(3))
                .andExpect(jsonPath("$._links.previous.href").value("http://localhost/api/summaries/2026-10"))
                .andExpect(jsonPath("$._links.next.href").value("http://localhost/api/summaries/2026-12"))
                .andExpect(jsonPath("$._links.by-member.href").exists())
                .andExpect(jsonPath("$._links.by-category.href").exists())
                .andExpect(jsonPath("$.cards[?(@.cardId == %d)].total".formatted(f.card)).value(120.0));
        mockMvc.perform(authorized(get("/api/summaries/2026-11/members")))
                .andExpect(jsonPath("$[?(@.memberId == %d)].card".formatted(f.main)).value(120.0));
        mockMvc.perform(authorized(get("/api/summaries/2026-11/categories")))
                .andExpect(jsonPath("$[0].amount").value(120.0));
    }

    @Test
    @DisplayName("RF-15: the member filter narrows the summary")
    void memberFilter() throws Exception {
        mockMvc.perform(authorized(get("/api/summaries/2026-11?member=" + f.thirdParty)))
                .andExpect(jsonPath("$.memberId").value(f.thirdParty))
                .andExpect(jsonPath("$.cardTotal").value(0.0))
                .andExpect(jsonPath("$.members.length()").value(1));
    }

    @Test
    @DisplayName("RF-36, IMP-31: an open bill offers import; a closed one offers mark-paid and becomes PAID")
    void cardBillLinks() throws Exception {
        String bill = "/api/cards/" + f.card + "/bills/2026-11";
        mockMvc.perform(authorized(get(bill)))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$._links.import.href").value("/api/imports"))
                .andExpect(jsonPath("$._links.mark-paid").doesNotExist())
                .andExpect(jsonPath("$._links.bill-entries.href").value("/api/bill-entries?billingPeriod=2026-11&card=" + f.card));
        mockMvc.perform(authorized(post(bill + "/payment"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("bill-not-closed"));
        var stored = bills.findByCardIdAndBillingPeriod(f.card, NOV).orElseThrow();
        stored.recordFile(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 10), Money.of("120.00"), true);
        bills.save(stored);
        mockMvc.perform(authorized(get("/api/card-bills?billingPeriod=2026-11")))
                .andExpect(jsonPath("$._embedded.cardBills[?(@.cardId == %d)].status".formatted(f.card)).value("CLOSED"))
                .andExpect(jsonPath("$._embedded.cardBills[?(@.cardId == %d)].fileTotal".formatted(f.card)).value(120.0));
        mockMvc.perform(authorized(get(bill))).andExpect(jsonPath("$._links.mark-paid.href").exists());
        mockMvc.perform(authorized(post(bill + "/payment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$._links.mark-paid").doesNotExist())
                .andExpect(jsonPath("$._links.import").doesNotExist());
    }
}
