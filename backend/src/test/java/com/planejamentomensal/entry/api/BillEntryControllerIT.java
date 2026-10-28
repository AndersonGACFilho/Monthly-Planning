package com.planejamentomensal.entry.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static com.planejamentomensal.shared.api.JsonBodies.read;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.imports.application.ConfirmImportUseCase;
import com.planejamentomensal.imports.application.PreviewImportUseCase;
import com.planejamentomensal.imports.infra.itau.Fixtures;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.domain.BillingPeriod;
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
class BillEntryControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private PreviewImportUseCase preview;
    @Autowired private ConfirmImportUseCase confirm;

    private LedgerFixture f;

    @BeforeEach
    void importOctober() {
        f = new LedgerFixture(members, cards, "3");
        var batch = preview.preview(f.card, BillingPeriod.of(2026, 10), Fixtures.CLOSED_PDF, Fixtures.bytes(Fixtures.CLOSED_PDF));
        confirm.confirm(batch.id(), false);
    }

    private String firstEntry(String query) throws Exception {
        var result = mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-10&card=" + f.card + query))).andReturn();
        return read(result, "$._embedded.billEntries[0]._links.self.href");
    }

    @Test
    @DisplayName("RF-40, RF-41, API-06: entries of a period are listed with filters, sorted by entry date")
    void listsAndFilters() throws Exception {
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-10&card=" + f.card)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(79))
                .andExpect(jsonPath("$._embedded.billEntries[0].entryDate").value("2025-10-14"));
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-10&card=" + f.card + "&type=n/N")))
                .andExpect(jsonPath("$._embedded.billEntries[*].installment", everyItem(org.hamcrest.Matchers.matchesPattern("\\d{2}/\\d{2}"))));
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-10&card=" + f.card + "&type=-&member=" + f.main)))
                .andExpect(jsonPath("$._embedded.billEntries[*].installment", everyItem(org.hamcrest.Matchers.is("-"))));
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-10&sort=amount,desc&size=1")))
                .andExpect(jsonPath("$._embedded.billEntries[0].amount").value(738.17))
                .andExpect(jsonPath("$._links.next.href").exists());
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-11&card=" + f.card + "&status=PROJECTED")))
                .andExpect(jsonPath("$._embedded.billEntries[0]._links.confirm.href").exists());
    }

    @Test
    @DisplayName("RF-46, RF-48, RF-50, RF-51: rename all entries with the description, find by alias and restore")
    void renameAllAndRestore() throws Exception {
        String self = firstEntry("&q=POSTO AGRONOMIA");
        mockMvc.perform(authorized(get(self))).andExpect(jsonPath("$._links.restore-original").doesNotExist());
        mockMvc.perform(authorized(put(self + "/alias")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Posto do bairro\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayDescription").value("Posto do bairro"))
                .andExpect(jsonPath("$.originalDescription").value("POSTO AGRONOMIAPORTO AL"))
                .andExpect(jsonPath("$._links.restore-original.href").exists());
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-10&q=bairro")))
                .andExpect(jsonPath("$.page.totalElements").value(4))
                .andExpect(jsonPath("$._embedded.billEntries[*].displayDescription", everyItem(org.hamcrest.Matchers.is("Posto do bairro"))));
        mockMvc.perform(authorized(delete(self + "/alias")))
                .andExpect(jsonPath("$.displayDescription").value("POSTO AGRONOMIAPORTO AL"));
    }

    @Test
    @DisplayName("RF-48: renaming only this entry keeps the others with the original")
    void renameOnlyThis() throws Exception {
        String self = firstEntry("&q=PETLOVE SAUD");
        mockMvc.perform(authorized(put(self + "/alias")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Banho do cachorro\",\"scope\":\"ONLY_THIS\"}"))
                .andExpect(jsonPath("$.displayDescription").value("Banho do cachorro"));
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-10&q=PETLOVE")))
                .andExpect(jsonPath("$._embedded.billEntries[*].displayDescription", hasItem("PETLOVE SAUD*Petlove SS")));
    }

    @Test
    @DisplayName("RF-43, RN-08: inline category and member edit applies to every installment of the purchase")
    void inlineEdit() throws Exception {
        var category = mockMvc.perform(authorized(post("/api/categories")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Viagem\",\"color\":\"#000000\",\"icon\":\"plane\"}")).andReturn();
        long categoryId = ((Number) read(category, "$.id")).longValue();
        String self = firstEntry("&q=BMB");
        var entry = mockMvc.perform(authorized(get(self))).andReturn();
        String purchase = read(entry, "$._links.purchase.href");
        mockMvc.perform(authorized(patch(self)).contentType(MediaType.APPLICATION_JSON).header("If-Match", "\"" + read(entry, "$.version") + "\"")
                        .content("{\"categoryId\":%d,\"splits\":[{\"memberId\":%d,\"percentage\":100}]}".formatted(categoryId, f.thirdParty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.splits[0].memberId").value(f.thirdParty));
        mockMvc.perform(authorized(get(purchase + "/installments")))
                .andExpect(jsonPath("$._embedded.billEntries.length()").value(2))
                .andExpect(jsonPath("$._embedded.billEntries[*].categoryId", everyItem(org.hamcrest.Matchers.is((int) categoryId))));
        mockMvc.perform(authorized(get(purchase))).andExpect(jsonPath("$._links.installments.href").exists());
    }

    @Test
    @DisplayName("RF-44, RF-45, RF-79c: manual installment purchase goes to the card's period, projects the rest and is validated")
    void manualEntry() throws Exception {
        mockMvc.perform(authorized(post("/api/bill-entries")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"cardId":%d,"purchaseDate":"2026-10-05","description":"Geladeira","amount":250.00,"installmentCount":4}
                        """.formatted(f.card)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billingPeriod").value("2026-11"))
                .andExpect(jsonPath("$.installment").value("01/04"))
                .andExpect(jsonPath("$.reviewStatus").value("REVIEWED"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2027-02&q=Geladeira")))
                .andExpect(jsonPath("$._embedded.billEntries[0].installment").value("04/04"))
                .andExpect(jsonPath("$._embedded.billEntries[0].status").value("PROJECTED"));
    }

    @Test
    @DisplayName("RF-44, API-09: a manual entry goes to the bill the user chose and offers replace and delete")
    void manualEntryOnChosenBill() throws Exception {
        mockMvc.perform(authorized(post("/api/bill-entries")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"cardId":%d,"purchaseDate":"2026-10-05","description":"Padaria","amount":12.50,"installmentCount":1,
                         "billingPeriod":"2026-10"}
                        """.formatted(f.card)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billingPeriod").value("2026-10"))
                .andExpect(jsonPath("$._links.replace-purchase.href").exists())
                .andExpect(jsonPath("$._links.delete.href").exists());
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-10&card=" + f.card + "&q=BMB")))
                .andExpect(jsonPath("$._embedded.billEntries[0]._links.replace-purchase").doesNotExist())
                .andExpect(jsonPath("$._embedded.billEntries[0]._links.delete").doesNotExist());
    }

    @Test
    @DisplayName("RF-44, RF-45: replacing a manual purchase moves its installments and keeps the member split")
    void replaceManualEntry() throws Exception {
        String self = createManual("Geladeira", 4);
        mockMvc.perform(authorized(patch(self)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"splits\":[{\"memberId\":%d,\"percentage\":50},{\"memberId\":%d,\"percentage\":50}]}"
                                .formatted(f.main, f.thirdParty)))
                .andExpect(status().isOk());
        var entry = mockMvc.perform(authorized(get(self))).andReturn();
        mockMvc.perform(authorized(put(self + "/purchase")).contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"" + read(entry, "$.version") + "\"").content("""
                        {"cardId":%d,"purchaseDate":"2026-11-20","description":"Geladeira nova","amount":500.00,
                         "installmentCount":2,"billingPeriod":"2026-12"}
                        """.formatted(f.card)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingPeriod").value("2026-12"))
                .andExpect(jsonPath("$.installment").value("01/02"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.splits.length()").value(2));
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-11&q=Geladeira")))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2027-01&q=Geladeira")))
                .andExpect(jsonPath("$._embedded.billEntries[0].installment").value("02/02"));
        mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2027-02&q=Geladeira")))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    @DisplayName("RF-26, RF-43: an inline split by amount must cover the installment amount")
    void inlineSplitByAmount() throws Exception {
        String self = createManual("Mercado", 1);
        String split = "{\"splits\":[{\"memberId\":%d,\"amount\":%s},{\"memberId\":%d,\"amount\":%s}]}";
        mockMvc.perform(authorized(patch(self)).contentType(MediaType.APPLICATION_JSON)
                        .content(split.formatted(f.main, "100.00", f.thirdParty, "100.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("review-split-incomplete"));
        mockMvc.perform(authorized(patch(self)).contentType(MediaType.APPLICATION_JSON)
                        .content(split.formatted(f.main, "100.00", f.thirdParty, "150.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.splits[0].amount").value(100.00))
                .andExpect(jsonPath("$.splits[1].amount").value(150.00))
                .andExpect(jsonPath("$.splits[1].percentage").doesNotExist());
    }

    @Test
    @DisplayName("RF-44: deleting a manual purchase removes every installment")
    void deleteManualEntry() throws Exception {
        String self = createManual("Sofá", 3);
        mockMvc.perform(authorized(delete(self))).andExpect(status().isNoContent());
        for (String period : new String[] {"2026-11", "2026-12", "2027-01"}) {
            mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=" + period + "&q=Sof")))
                    .andExpect(jsonPath("$.page.totalElements").value(0));
        }
    }

    @Test
    @DisplayName("RF-44: an imported entry cannot be replaced or deleted")
    void importedEntryIsNotManual() throws Exception {
        String self = firstEntry("&q=BMB");
        mockMvc.perform(authorized(delete(self)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("entry-not-manual"));
        mockMvc.perform(authorized(put(self + "/purchase")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"cardId":%d,"purchaseDate":"2026-10-05","description":"X","amount":1.00,"installmentCount":1}
                        """.formatted(f.card)))
                .andExpect(status().isConflict());
        mockMvc.perform(authorized(get(self))).andExpect(status().isOk());
    }

    private String createManual(String description, int installments) throws Exception {
        var created = mockMvc.perform(authorized(post("/api/bill-entries")).contentType(MediaType.APPLICATION_JSON).content("""
                        {"cardId":%d,"purchaseDate":"2026-10-05","description":"%s","amount":250.00,"installmentCount":%d}
                        """.formatted(f.card, description, installments)))
                .andExpect(status().isCreated()).andReturn();
        return read(created, "$._links.self.href");
    }

    @Test
    @DisplayName("API-09: a projected installment can be confirmed by hand once")
    void confirmProjected() throws Exception {
        var projected = mockMvc.perform(authorized(get("/api/bill-entries?billingPeriod=2026-11&status=PROJECTED&card=" + f.card))).andReturn();
        String confirmHref = read(projected, "$._embedded.billEntries[0]._links.confirm.href");
        mockMvc.perform(authorized(post(confirmHref)))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$._links.confirm").doesNotExist());
        mockMvc.perform(authorized(post(confirmHref))).andExpect(status().isConflict());
    }
}
