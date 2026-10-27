package com.planejamentomensal.entry.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.time.LocalDate;
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
class PurchaseControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;

    @Test
    @DisplayName("RF-43, RN-04: a purchase lists all its installments, confirmed and projected")
    void installments() throws Exception {
        LedgerFixture f = new LedgerFixture(members, cards, "9");
        BillEntry tv = ledger.addPurchase(null, f.purchase("TV LOJA", "90.00", LocalDate.of(2026, 10, 5), BillingPeriod.of(2026, 11),
                1, 3, "PURCHASE"));
        long purchase = tv.purchase().id();
        mockMvc.perform(authorized(get("/api/purchases/" + purchase)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalDescription").value("TV LOJA"))
                .andExpect(jsonPath("$.installmentCount").value(3))
                .andExpect(jsonPath("$._links.installments.href").exists());
        mockMvc.perform(authorized(get("/api/purchases/" + purchase + "/installments")))
                .andExpect(jsonPath("$._embedded.billEntries.length()").value(3))
                .andExpect(jsonPath("$._embedded.billEntries[1].status").value("PROJECTED"));
        mockMvc.perform(authorized(get("/api/purchases/999999"))).andExpect(status().isNotFound());
    }
}
