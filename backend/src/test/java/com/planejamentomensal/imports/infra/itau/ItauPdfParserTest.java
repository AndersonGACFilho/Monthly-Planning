package com.planejamentomensal.imports.infra.itau;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.imports.domain.BillFormat;
import com.planejamentomensal.imports.domain.ParsedBill;
import com.planejamentomensal.imports.domain.RawBillLine;
import com.planejamentomensal.imports.domain.RawLineKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ItauPdfParserTest {

    private static ParsedBill bill;

    @BeforeAll
    static void parse() {
        bill = new ItauPdfParser().parse(Fixtures.bytes(Fixtures.CLOSED_PDF));
    }

    private static RawBillLine line(String description, String amount) {
        return bill.lines().stream()
                .filter(l -> l.description().equals(description) && l.amount().equals(Money.of(amount)))
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("RF-64: page 1 gives card final, due date, issue date, next closing and bill total")
    void header() {
        assertThat(bill.format()).isEqualTo(BillFormat.PDF);
        assertThat(bill.open()).isFalse();
        assertThat(bill.cardLastDigits()).isEqualTo("4102");
        assertThat(bill.dueDate()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(bill.billingPeriod()).isEqualTo(BillingPeriod.of(2026, 10));
        assertThat(bill.closingDate()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(bill.nextClosingForecast()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(bill.fileTotal()).isEqualTo(Money.of("6514.13"));
    }

    @Test
    @DisplayName("RF-64a, RF-64c: both columns of every page are read without mixing them")
    void readsBothColumns() {
        assertThat(bill.lines()).hasSize(79);
        assertThat(bill.lines()).extracting(RawBillLine::sequence).doesNotHaveDuplicates();
        assertThat(line("EC *PARAMOUNTPLUSOS", "24.43").purchaseDate()).isEqualTo(LocalDate.of(2026, 8, 28));
        assertThat(line("99Food *CASA DE CARNEPo", "42.84").purchaseDate()).isEqualTo(LocalDate.of(2026, 9, 27));
        assertThat(line("FormulaAnimalG", "69.00").installmentLabel()).isEqualTo("01/02");
    }

    @Test
    @DisplayName("RF-64b: second line gives the Itaú category, kept as a suggestion, and the holder is the section title")
    void categoryAndHolder() {
        RawBillLine fruit = line("FRUTEIRA DA ECONOMIA LP", "55.53");
        assertThat(fruit.bankCategory()).isEqualTo("supermercado");
        assertThat(fruit.note()).isEqualTo("supermercado PORTO ALEGRE");
        assertThat(fruit.holderName()).isEqualTo("MARIANA S F SILVA");
    }

    @Test
    @DisplayName("RF-64c: payments, charges, limits and legal texts are not read")
    void ignoredSections() {
        assertThat(bill.lines()).noneMatch(l -> l.description().startsWith("Pagamento"));
        assertThat(bill.lines()).noneMatch(l -> l.amount().equals(Money.of("17795.00")));
        assertThat(bill.lines()).noneMatch(l -> l.amount().equals(Money.of("-5385.41")));
    }

    @Test
    @DisplayName("RF-64d: international purchase in reais with the dollar line as note, and IOF as its own line")
    void international() {
        RawBillLine openai = line("OPENAI *CHATGPT SUBSCRS", "107.16");
        assertThat(openai.note()).contains("Dólar de Conversão R$ 5,47");
        RawBillLine iof = bill.lines().stream().filter(l -> l.kind() == RawLineKind.IOF).findFirst().orElseThrow();
        assertThat(iof.amount()).isEqualTo(Money.of("3.75"));
        assertThat(iof.description()).isEqualTo("Repasse de IOF");
    }

    @Test
    @DisplayName("RF-64e: PIX installment keeps principal and interest in the note")
    void pixInstallment() {
        RawBillLine pix = line("PIX DEPARTAMEN", "104.45");
        assertThat(pix.installmentLabel()).isEqualTo("01/05");
        assertThat(pix.kind()).isEqualTo(RawLineKind.PURCHASE);
        assertThat(pix.note()).isEqualTo("Principal (R$ 83,09) + Juros (R$ 21,36)");
    }

    @Test
    @DisplayName("RF-67: credits are negative and fees are marked")
    void creditsAndFees() {
        assertThat(bill.lines()).filteredOn(l -> l.kind() == RawLineKind.CREDIT)
                .extracting(RawBillLine::amount).containsExactly(Money.of("-33.25"), Money.of("-33.25"));
        assertThat(line("Mensalidade - Plano do", "66.50").kind()).isEqualTo(RawLineKind.FEE);
    }

    @Test
    @DisplayName("RF-64f: installment year comes from the closing date (14/10 12/12 on the October bill is 2025)")
    void installmentYear() {
        assertThat(line("AMAZONMKTPLC*E", "201.62").purchaseDate()).isEqualTo(LocalDate.of(2025, 10, 14));
        assertThat(line("MERCADOLIVRE*7", "56.18").purchaseDate()).isEqualTo(LocalDate.of(2026, 2, 12));
    }

    @Test
    @DisplayName("RF-64g: future installments are read for checking only")
    void futureInstallments() {
        List<RawBillLine> future = bill.futureInstallments();
        assertThat(future).hasSize(16);
        assertThat(future).extracting(RawBillLine::installmentLabel).contains("09/09", "02/02", "02/05");
        assertThat(future.stream().filter(l -> l.description().equals("MERCADOLIVRE*M")).findFirst().orElseThrow().amount())
                .isEqualTo(Money.of("48.01"));
        assertThat(bill.lines()).noneMatch(l -> l.installmentLabel().equals("09/09"));
    }

    @Test
    @DisplayName("RF-64h: section totals match \"Total dos lançamentos atuais\"")
    void totalsMatch() {
        assertThat(bill.sectionTotals()).hasSize(3).allMatch(t -> t.matches());
        assertThat(bill.entriesTotal()).isEqualTo(Money.of("6514.13"));
        assertThat(bill.computedTotal()).isEqualTo(Money.of("6514.13"));
        assertThat(bill.totalsMatch()).isTrue();
    }

    @Test
    @DisplayName("RF-67: personal data of the file is not read")
    void noPersonalData() {
        assertThat(bill.lines()).allSatisfy(l -> {
            assertThat(l.description() + l.note()).doesNotContain("123.456.789", "RUA EXEMPLO", "01001-000");
        });
    }

    @Test
    @DisplayName("RF-61: a file that is not an Itaú PDF is rejected")
    void rejectsOtherFiles() {
        assertThatThrownBy(() -> new ItauPdfParser().parse("not a pdf".getBytes()))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("invalid-bill-file");
    }
}
