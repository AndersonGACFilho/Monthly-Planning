package com.planejamentomensal.statement.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryCommands;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.statement.application.StatementFixture;
import org.hamcrest.Matchers;
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
class StatementControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private EntryCommands entries;
    @Autowired private RecurrenceCommands recurrence;
    @Autowired private VariableExpenseCommands expenses;

    private StatementFixture s;

    @BeforeEach
    void setUp() {
        s = new StatementFixture(members, cards, ledger, entries, recurrence, expenses, "2");
    }

    @Test
    @DisplayName("RF-85, IMP-37: the statement preview has totals, pending items and format links; send-whatsapp needs a phone")
    void statement() throws Exception {
        String base = "/api/statements/2026-11/members/" + s.f.thirdParty;
        mockMvc.perform(authorized(get(base)).param("includeFuture", "false").param("note", "Pague logo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(455.5))
                .andExpect(jsonPath("$.language").value("pt-BR"))
                .andExpect(jsonPath("$.pendingItems.length()").value(2))
                .andExpect(jsonPath("$.lines[0].percent").value(50))
                .andExpect(jsonPath("$._links.pdf.href").value(Matchers.containsString("/pdf?showCategories=true&useAlias=true&includeFuture=false&note=Pague")))
                .andExpect(jsonPath("$._links.png.href").exists())
                .andExpect(jsonPath("$._links.xlsx.href").exists())
                .andExpect(jsonPath("$._links.preview.href").exists())
                .andExpect(jsonPath("$._links.send-whatsapp").doesNotExist())
                .andExpect(jsonPath("$._links.send-email").doesNotExist());
        mockMvc.perform(authorized(get(base + "/html")).param("note", "Pague logo"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(Matchers.containsString("Pague logo")));
        var pdf = mockMvc.perform(authorized(get(base + "/pdf"))).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", Matchers.containsString("extrato-t2-2026-11.pdf")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        mockMvc.perform(authorized(get(base + "/png"))).andExpect(content().contentType(MediaType.IMAGE_PNG));
        mockMvc.perform(authorized(get(base + "/csv?language=en-US")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("statement-t2-2026-11.csv")));
        mockMvc.perform(authorized(get(base + "/xlsx"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("RF-79b, RF-91, RF-92: batch send warns about pending entries, then records history with resend")
    void deliveries() throws Exception {
        String body = """
                {"billingPeriod":"2026-11","memberIds":[%d],"channel":"FOLDER"%s}
                """;
        mockMvc.perform(authorized(post("/api/statement-deliveries")).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(s.f.thirdParty, "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("statement-has-pending"));
        mockMvc.perform(authorized(post("/api/statement-deliveries")).contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted(s.f.thirdParty, ",\"sendAnyway\":true,\"language\":\"en-US\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SENT"));
        mockMvc.perform(authorized(get("/api/statement-deliveries?billingPeriod=2026-11&member=" + s.f.thirdParty)))
                .andExpect(jsonPath("$._embedded.deliveries[0].language").value("en-US"))
                .andExpect(jsonPath("$._embedded.deliveries[0].outdated").value(false))
                .andExpect(jsonPath("$._embedded.deliveries[0]._links.resend.href").exists())
                .andExpect(jsonPath("$._embedded.deliveries[0]._links.mark-sent").doesNotExist());
        mockMvc.perform(authorized(get("/api/members/" + s.f.thirdParty))).andExpect(jsonPath("$._links.delete").doesNotExist());
    }

    @Test
    @DisplayName("IMP-36, TPL-03: template links per state, live preview with sample data and write-only SMTP password")
    void templates() throws Exception {
        mockMvc.perform(authorized(get("/api/templates")))
                .andExpect(jsonPath("$._embedded.templates[0].isDefault").value(true))
                .andExpect(jsonPath("$._embedded.templates[0]._links.make-default").doesNotExist())
                .andExpect(jsonPath("$._embedded.templates[0]._links.versions.href").exists())
                .andExpect(jsonPath("$._embedded.templates[0]._links.preview.href").exists());
        String settings = """
                {"primaryColor":"#123456","secondaryColor":"#654321","font":"NOTO_SERIF","blocks":["SUMMARY","CARDS"],
                 "texts":{"pt-BR":{"header":"Olá {{member.name}}"}}}
                """;
        var created = mockMvc.perform(authorized(post("/api/templates")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Serifa\",\"settings\":" + settings + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.make-default.href").exists())
                .andReturn().getResponse().getHeader("Location");
        mockMvc.perform(authorized(put(created)).contentType(MediaType.APPLICATION_JSON).header("If-Match", "\"0\"")
                        .content("{\"name\":\"Serifa\",\"settings\":" + settings.replace("#123456", "#000000") + "}"))
                .andExpect(jsonPath("$.currentVersion").value(2));
        mockMvc.perform(authorized(get(created + "/versions"))).andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(authorized(post("/api/templates/preview")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settings\":" + settings + ",\"billingPeriod\":\"2026-11\",\"language\":\"en-US\"}"))
                .andExpect(jsonPath("$.fallback").value(true))
                .andExpect(jsonPath("$.html").value(Matchers.containsString("Olá Maria")));
        mockMvc.perform(authorized(post("/api/templates")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"settings\":" + settings.replace("{{member.name}}", "{{cpf}}") + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("template-variable-unknown"));
        mockMvc.perform(authorized(put("/api/templates/assignments/" + s.f.thirdParty)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"templateId\":" + created.substring(created.lastIndexOf('/') + 1) + "}")).andExpect(status().isNoContent());
        mockMvc.perform(authorized(get("/api/templates/assignments"))).andExpect(jsonPath("$." + s.f.thirdParty).exists());
        mockMvc.perform(authorized(put("/api/settings/smtp-password")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"abc\"}")).andExpect(jsonPath("$.configured").value(true));
        mockMvc.perform(authorized(get("/api/settings"))).andExpect(jsonPath("$.smtp-password").doesNotExist());
    }
}
