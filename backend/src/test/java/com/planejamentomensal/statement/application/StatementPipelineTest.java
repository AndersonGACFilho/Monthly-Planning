package com.planejamentomensal.statement.application;

import static com.planejamentomensal.statement.application.StatementFixture.NOV;
import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryCommands;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.statement.domain.MemberStatement;
import com.planejamentomensal.statement.domain.StatementFingerprint;
import com.planejamentomensal.statement.domain.StatementOptions;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StatementPipelineTest {

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private EntryCommands entries;
    @Autowired private RecurrenceCommands recurrence;
    @Autowired private VariableExpenseCommands expenses;
    @Autowired private MemberStatementBuilder builder;
    @Autowired private StatementService service;

    private StatementFixture s;

    @BeforeEach
    void setUp() {
        s = new StatementFixture(members, cards, ledger, entries, recurrence, expenses, "4");
    }

    @Test
    @DisplayName("RF-81, RF-82: the statement has the member share of each card entry, fixed and variable expenses and future installments")
    void builds() {
        MemberStatement st = builder.build(s.f.thirdParty, NOV, StatementOptions.DEFAULT);
        assertThat(st.cards()).singleElement().satisfies(g -> {
            assertThat(g.lines()).extracting(MemberStatement.CardLine::description).containsExactly("MERCADO BOM", "FARMACIA");
            assertThat(g.lines().getFirst().amount()).isEqualTo(Money.of("50.00"));
            assertThat(g.lines().getFirst().percent()).isEqualByComparingTo("50");
            assertThat(g.lines().get(1).percent()).isNull();
            assertThat(g.total()).isEqualTo(Money.of("130.00"));
        });
        assertThat(st.fixedTotal()).isEqualTo(Money.of("300.00"));
        assertThat(st.variableTotal()).isEqualTo(Money.of("25.50"));
        assertThat(st.total()).isEqualTo(Money.of("455.50"));
        assertThat(st.committed()).singleElement().satisfies(c -> assertThat(c.amount()).isEqualTo(Money.of("80.00")));
        assertThat(st.dueDate()).isNotNull();
        assertThat(st.pendingItems()).hasSize(2);
        MemberStatement house = builder.build(s.f.main, NOV, StatementOptions.DEFAULT);
        assertThat(house.cardTotal()).isEqualTo(Money.of("250.00"));
    }

    @Test
    @DisplayName("RF-83, RF-93: options hide future installments; the fingerprint ignores the issue date but not the content")
    void options() {
        MemberStatement noFuture = builder.build(s.f.thirdParty, NOV, new StatementOptions(false, true, false));
        assertThat(noFuture.committed()).isEmpty();
        assertThat(noFuture.cards().getFirst().lines()).allMatch(l -> l.category() == null);
        assertThat(StatementFingerprint.of(builder.build(s.f.thirdParty, NOV, StatementOptions.DEFAULT)))
                .isEqualTo(StatementFingerprint.of(builder.build(s.f.thirdParty, NOV, StatementOptions.DEFAULT)))
                .isNotEqualTo(StatementFingerprint.of(noFuture));
    }

    @Test
    @DisplayName("TPL-01, I18N-08, I18N-10: one HTML in the member language with localized labels, money and dates")
    void rendersBothLanguages() {
        PreparedStatement pt = service.prepare(new StatementRequest(s.f.thirdParty, NOV, StatementOptions.DEFAULT, null, null, null));
        assertThat(pt.rendered().language()).isEqualTo("pt-BR");
        assertThat(pt.rendered().html()).contains("Total devido", "R$ 455,50", "Novembro de 2026", "Lançamentos no cartão",
                "Parcelas futuras", "Alguns lançamentos ainda estão pendentes");
        assertThat(pt.rendered().subject()).isEqualTo("Extrato Novembro de 2026: R$ 455,50");
        PreparedStatement en = service.prepare(new StatementRequest(s.f.thirdParty, NOV, StatementOptions.DEFAULT, "en-US", null, null));
        assertThat(en.rendered().html()).contains("Total due", "R$455.50", "November 2026", "Card entries");
        assertThat(en.rendered().whatsappText()).startsWith("Hi Tia!");
        assertThat(en.rendered().fileBaseName()).isEqualTo("statement-t4-2026-11");
    }

    @Test
    @DisplayName("RF-84, POR-05: PDF with embedded fonts, PNG image and XLSX/CSV with the same lines")
    void formats() throws Exception {
        PreparedStatement pt = service.prepare(new StatementRequest(s.f.thirdParty, NOV, StatementOptions.DEFAULT, null, null, null));
        byte[] pdf = service.pdf(pt);
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("MERCADO BOM", "R$ 455,50");
            Set<String> fonts = new HashSet<>();
            for (PDPage page : doc.getPages()) {
                for (var name : page.getResources().getFontNames()) {
                    var font = page.getResources().getFont(name);
                    fonts.add(font.getName());
                    assertThat(font.isEmbedded()).isTrue();
                }
            }
            assertThat(fonts).anyMatch(n -> n.contains("NotoSans"));
        }
        var image = ImageIO.read(new ByteArrayInputStream(service.png(pt)));
        assertThat(image.getWidth()).isGreaterThan(500);
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(service.xlsx(pt)))) {
            assertThat(book.getSheetAt(0).getRow(3).getCell(3).getStringCellValue()).isEqualTo("MERCADO BOM");
        }
        String csv = new String(service.csv(pt), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("\"MERCADO BOM\"", "\"50,00\"", "\"Remédio\"");
    }
}
