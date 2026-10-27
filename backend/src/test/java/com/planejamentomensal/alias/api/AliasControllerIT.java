package com.planejamentomensal.alias.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static com.planejamentomensal.shared.api.JsonBodies.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.time.LocalDate;
import org.hamcrest.Matchers;
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
class AliasControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;

    @Test
    @DisplayName("RF-46, RN-24: renaming creates an alias that lists its descriptions and groups another one")
    void aliases() throws Exception {
        LedgerFixture f = new LedgerFixture(members, cards, "8");
        BillEntry entry = ledger.addPurchase(null, f.purchase("PADARIA DO ZE LTDA", "12.00", LocalDate.of(2026, 10, 5),
                BillingPeriod.of(2026, 11), null, null, "PURCHASE"));
        mockMvc.perform(authorized(put("/api/bill-entries/" + entry.id() + "/alias")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Padaria do Zé\",\"scope\":\"ALL\"}"))
                .andExpect(status().isOk());
        var list = mockMvc.perform(authorized(get("/api/aliases")))
                .andExpect(jsonPath("$._embedded.aliases[?(@.displayName == 'Padaria do Zé')].patterns[0]")
                        .value("PADARIADOZELTDA"))
                .andReturn();
        String group = read(list, "$._embedded.aliases[0]._links.group.href");
        mockMvc.perform(authorized(post(group)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalDescription\":\"PADARIA ZE FILIAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patterns", Matchers.hasItem("PADARIAZEFILIAL")));
    }
}
