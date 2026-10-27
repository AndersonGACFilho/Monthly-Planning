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
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ItauXlsxParserTest {

    private static ParsedBill bill;

    @BeforeAll
    static void parse() {
        bill = new ItauXlsxParser().parse(Fixtures.bytes(Fixtures.OPEN_XLSX));
    }

    private static RawBillLine line(String description, String amount) {
        return bill.lines().stream()
                .filter(l -> l.description().equals(description) && l.amount().equals(Money.of(amount)))
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("RF-63: period, due date and card final are found by text, not by row number")
    void header() {
        assertThat(bill.format()).isEqualTo(BillFormat.XLSX);
        assertThat(bill.open()).isTrue();
        assertThat(bill.billingPeriod()).isEqualTo(BillingPeriod.of(2026, 11));
        assertThat(bill.dueDate()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(bill.cardLastDigits()).isEqualTo("4102");
    }

    @Test
    @DisplayName("RF-63a, RF-63b: columns by header name, read until the first empty row, total computed")
    void lines() {
        assertThat(bill.lines()).hasSize(88);
        assertThat(bill.computedTotal()).isEqualTo(Money.of("11916.21"));
        assertThat(bill.entriesTotal()).isNull();
        assertThat(bill.totalsMatch()).isTrue();
        RawBillLine first = bill.lines().getFirst();
        assertThat(first.description()).isEqualTo("Caoa Chery  Porto Alegporto Alegrebra");
        assertThat(first.purchaseDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(first.holderName()).isEqualTo("Mariana S F Silva");
        assertThat(first.cardLastDigits()).isEqualTo("4102");
    }

    @Test
    @DisplayName("RF-63c: \"Parcela N de M\" becomes N/M and empty is a single purchase")
    void installments() {
        assertThat(line("Bmb    *Equatorialporto Alegrebra", "276.77").installmentLabel()).isEqualTo("02/02");
        assertThat(line("Mercadolivre*mercadoljaragua Do Sbra", "48.01").purchaseDate()).isEqualTo(LocalDate.of(2026, 8, 30));
        assertThat(bill.lines().stream().filter(l -> !l.isInstallment()).count()).isPositive();
    }

    @Test
    @DisplayName("RF-63d: negative \"Pagamento…\" is not an entry; other negatives are credits")
    void paymentsAndCredits() {
        assertThat(bill.lines()).noneMatch(l -> l.description().startsWith("Pagamento"));
        assertThat(bill.lines()).filteredOn(l -> l.kind() == RawLineKind.CREDIT).hasSize(2)
                .allMatch(l -> l.description().startsWith("Redução Mensalidade"));
        assertThat(line("Mensalidade - Plano Do Cartão", "66.50").kind()).isEqualTo(RawLineKind.FEE);
    }

    @Test
    @DisplayName("RF-63e: \"Iof Internacional - <description>\" is an IOF line linked to that purchase")
    void iof() {
        RawBillLine iof = bill.lines().stream().filter(l -> l.kind() == RawLineKind.IOF).findFirst().orElseThrow();
        assertThat(iof.amount()).isEqualTo(Money.of("3.72"));
        assertThat(iof.relatedDescription()).isEqualTo("Openai *Chatgpt Subscrsan Franciscousa");
        assertThat(bill.lines()).anyMatch(l -> l.description().equals(iof.relatedDescription())
                && l.purchaseDate().equals(iof.purchaseDate()));
    }

    @Test
    @DisplayName("RF-66: each line keeps the card final, including additional finals")
    void finals() {
        assertThat(bill.lines()).extracting(RawBillLine::cardLastDigits).containsOnly("4102", "4090", "9103");
    }

    @Test
    @DisplayName("RF-67: branch, account and holder full name of the header are not read into lines")
    void noPersonalData() {
        assertThat(bill.lines()).allSatisfy(l -> assertThat(l.description()).doesNotContain("12345-6", "Souza Ferreira"));
    }

    @Test
    @DisplayName("RF-61: workbook without the Itaú structure is rejected")
    void rejectsOtherWorkbooks() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet("x").createRow(0).createCell(0).setCellValue("hello");
            workbook.write(out);
            assertThatThrownBy(() -> new ItauXlsxParser().parse(out.toByteArray()))
                    .isInstanceOf(InvalidInputException.class);
        }
        assertThatThrownBy(() -> new ItauXlsxParser().parse(new byte[] {1, 2, 3})).isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("RNF-03: a 300-line bill is read in well under 5 seconds")
    void performance() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Lançamentos");
            sheet.createRow(1).createCell(1).setCellValue("Fatura Aberta - Novembro/2026");
            sheet.createRow(2).createCell(7).setCellValue("Vencimento");
            sheet.createRow(3).createCell(7).setCellValue("09/11/2026");
            var header = sheet.createRow(5);
            String[] names = {"Data", "Lançamento", "Parcelamento", "Valor", "", "Titularidade", "Nome", "Tipo do cartão", "Número do cartão"};
            for (int i = 0; i < names.length; i++) {
                header.createCell(i + 1).setCellValue(names[i]);
            }
            for (int r = 0; r < 300; r++) {
                var row = sheet.createRow(6 + r);
                row.createCell(1).setCellValue("1%d/10/2026".formatted(r % 10));
                row.createCell(2).setCellValue("Loja " + r);
                row.createCell(4).setCellValue(10.5 + r);
                row.createCell(9).setCellValue("****4102");
            }
            workbook.write(out);
            long start = System.nanoTime();
            ParsedBill large = new ItauXlsxParser().parse(out.toByteArray());
            long millis = (System.nanoTime() - start) / 1_000_000;
            assertThat(large.lines()).hasSize(300);
            assertThat(millis).isLessThan(5000);
        }
    }
}
