package com.planejamentomensal.imports.infra.itau;

import com.planejamentomensal.imports.application.BillParser;
import com.planejamentomensal.imports.domain.BillFormat;
import com.planejamentomensal.imports.domain.ParsedBill;
import com.planejamentomensal.imports.domain.PurchaseYearResolver;
import com.planejamentomensal.imports.domain.RawBillLine;
import com.planejamentomensal.imports.domain.RawLineKind;
import com.planejamentomensal.imports.domain.SectionTotal;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.domain.Money;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.stereotype.Component;

/**
 * Reads the closed Itaú bill as PDF (RF-64 to RF-64h).
 *
 * <p>Page 1 gives the card final, due date, issue date, next closing forecast and "Total desta fatura".
 * The following pages have two columns, each extracted by area with {@link PDFTextStripperByArea} (RF-64a):
 * extracting the whole page by position mixes the columns. The column texts are then read in order as one
 * stream of lines, section by section.</p>
 *
 * @since 1.0
 */
@Component
public class ItauPdfParser implements BillParser {

    /** X coordinate (points) that separates the two columns of the entry pages. */
    static final float COLUMN_SPLIT = 340f;

    private static final Pattern ENTRY = Pattern.compile("^(\\d{2})/(\\d{2})\\s+(.+?)\\s+(-?[\\d.]*\\d,\\d{2})$");
    private static final Pattern CARD_FINAL = Pattern.compile("(?:\\d{4}|X{4})\\.X{4}\\.X{4}\\.(\\d{4})");
    private static final Pattern AMOUNT_AT_END = Pattern.compile("(-?[\\d.]*\\d,\\d{2})\\s*$");
    private static final Pattern HOLDER = Pattern.compile("^[A-ZÀ-Ý][A-ZÀ-Ý ]{2,}$");

    /** Sections of the entry pages (RF-64c). */
    private enum Section { NONE, IGNORED, PURCHASES, INTERNATIONAL, PRODUCTS, FUTURE }

    /**
     * Creates the parser.
     */
    public ItauPdfParser() {
        // stateless
    }

    @Override
    public BillFormat format() {
        return BillFormat.PDF;
    }

    @Override
    public ParsedBill parse(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return parse(document);
        } catch (IOException | RuntimeException e) {
            if (e instanceof InvalidInputException invalid) {
                throw invalid;
            }
            throw new InvalidInputException("invalid-bill-file", "PDF", e.getMessage());
        }
    }

    private ParsedBill parse(PDDocument document) throws IOException {
        if (document.getNumberOfPages() < 2) {
            throw new InvalidInputException("invalid-bill-file", "PDF", "too few pages");
        }
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(1);
        stripper.setEndPage(1);
        String first = stripper.getText(document);

        LocalDate dueDate = ItauText.dateAfter(first, "Vencimento").orElseThrow(() -> invalid("due date"));
        LocalDate issueDate = ItauText.dateAfter(first, "Emissão").orElse(dueDate.minusDays(10));
        LocalDate nextClosing = ItauText.dateAfter(first, "Previsão próx. Fechamento").orElse(null);
        Money fileTotal = ItauText.moneyAfter(first, "Total desta fatura").orElseThrow(() -> invalid("bill total"));
        Matcher cardFinal = CARD_FINAL.matcher(first);
        String lastDigits = cardFinal.find() ? cardFinal.group(1) : null;

        List<String> lines = new ArrayList<>();
        for (int index = 1; index < document.getNumberOfPages(); index++) {
            lines.addAll(columns(document.getPage(index)));
        }
        Reader reader = new Reader(issueDate, nextClosing == null ? issueDate.plusMonths(1) : nextClosing);
        lines.forEach(reader::accept);
        reader.finish();
        if (reader.entries.isEmpty()) {
            throw invalid("no entries");
        }
        return new ParsedBill(BillFormat.PDF, false, BillingPeriod.ofDueDate(dueDate), dueDate, issueDate, nextClosing,
                lastDigits, fileTotal, reader.entriesTotal, reader.sectionTotals, reader.entries, reader.future);
    }

    private static List<String> columns(PDPage page) throws IOException {
        PDRectangle box = page.getCropBox();
        PDFTextStripperByArea areas = new PDFTextStripperByArea();
        areas.setSortByPosition(true);
        areas.addRegion("left", new Rectangle2D.Float(0, 0, COLUMN_SPLIT, box.getHeight()));
        areas.addRegion("right", new Rectangle2D.Float(COLUMN_SPLIT, 0, box.getWidth() - COLUMN_SPLIT, box.getHeight()));
        areas.extractRegions(page);
        List<String> lines = new ArrayList<>();
        for (String region : List.of("left", "right")) {
            for (String line : areas.getTextForRegion(region).split("\\R")) {
                String trimmed = line.replace(' ', ' ').strip().replaceAll("\\s{2,}", " ");
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        }
        return lines;
    }

    private static InvalidInputException invalid(String what) {
        return new InvalidInputException("invalid-bill-file", "PDF", what);
    }

    /** Line-by-line state machine over the column texts. */
    private static final class Reader {

        private final LocalDate closing;
        private final LocalDate nextClosing;
        private final List<RawBillLine> entries = new ArrayList<>();
        private final List<RawBillLine> future = new ArrayList<>();
        private final List<SectionTotal> sectionTotals = new ArrayList<>();
        private Money entriesTotal;
        private Section section = Section.NONE;
        private String holder;
        private boolean expectHolder;
        private RawBillLine pending;
        private final StringBuilder pendingNote = new StringBuilder();
        private boolean pendingHasDetail;
        private Money purchasesSum = Money.ZERO;
        private Money internationalSum = Money.ZERO;
        private Money productsSum = Money.ZERO;
        private int sequence;

        Reader(LocalDate closing, LocalDate nextClosing) {
            this.closing = closing;
            this.nextClosing = nextClosing;
        }

        void accept(String line) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (startSection(lower)) {
                return;
            }
            if (lower.startsWith("data ") || lower.equals("continua...")) {
                return;
            }
            if (expectHolder && HOLDER.matcher(line).matches()) {
                holder = line;
                expectHolder = false;
                return;
            }
            if (total(line, lower)) {
                return;
            }
            if (section == Section.IGNORED || section == Section.NONE) {
                return;
            }
            Matcher entry = ENTRY.matcher(line);
            if (entry.matches()) {
                flush();
                startEntry(entry);
            } else if (pending != null) {
                detail(line);
            }
        }

        private boolean startSection(String lower) {
            Section next = null;
            if (lower.startsWith("lançamentos: compras e saques")) {
                next = Section.PURCHASES;
            } else if (lower.startsWith("lançamentos internacionais")) {
                next = Section.INTERNATIONAL;
            } else if (lower.startsWith("lançamentos: produtos e serviços")) {
                next = Section.PRODUCTS;
            } else if (lower.startsWith("compras parceladas - próximas faturas")) {
                next = Section.FUTURE;
            } else if (lower.startsWith("pagamentos efetuados") || lower.startsWith("limites de crédito")
                    || lower.startsWith("encargos cobrados") || lower.startsWith("novo teto de juros")) {
                next = Section.IGNORED;
            }
            if (next == null) {
                return false;
            }
            flush();
            section = next;
            expectHolder = next == Section.PURCHASES || next == Section.INTERNATIONAL;
            return true;
        }

        private boolean total(String line, String lower) {
            Matcher amount = AMOUNT_AT_END.matcher(line);
            boolean hasAmount = amount.find();
            if (lower.startsWith("lançamentos no cartão") && hasAmount) {
                flush();
                sectionTotals.add(new SectionTotal("Lançamentos no cartão", ItauText.money(amount.group(1)), purchasesSum));
                purchasesSum = Money.ZERO;
                return true;
            }
            if (lower.startsWith("repasse de iof") && hasAmount) {
                flush();
                Money iof = ItauText.money(amount.group(1));
                add(new RawBillLine(++sequence, RawLineKind.IOF, closing, "Repasse de IOF", null, null, iof, holder, null,
                        null, null, null));
                return true;
            }
            if (lower.startsWith("total lançamentos inter") && hasAmount) {
                flush();
                sectionTotals.add(new SectionTotal("Lançamentos internacionais", ItauText.money(amount.group(1)),
                        internationalSum));
                internationalSum = Money.ZERO;
                return true;
            }
            if (lower.startsWith("lançamentos produtos e serviços") && hasAmount) {
                flush();
                sectionTotals.add(new SectionTotal("Lançamentos: produtos e serviços", ItauText.money(amount.group(1)),
                        productsSum));
                productsSum = Money.ZERO;
                return true;
            }
            if ((lower.startsWith("l total dos lançamentos atuais") || lower.startsWith("total dos lançamentos atuais"))
                    && hasAmount) {
                flush();
                entriesTotal = ItauText.money(amount.group(1));
                return true;
            }
            if (lower.startsWith("total transações inter") || lower.startsWith("total dos pagamentos")
                    || lower.startsWith("p total dos pagamentos") || lower.startsWith("próxima fatura")
                    || lower.startsWith("demais faturas") || lower.startsWith("total para próximas faturas")) {
                flush();
                if (lower.startsWith("total para próximas faturas")) {
                    section = Section.IGNORED;
                }
                return true;
            }
            return false;
        }

        private void startEntry(Matcher entry) {
            DescriptionNormalizer.Split split = DescriptionNormalizer.splitInstallment(entry.group(3));
            Money amount = ItauText.money(entry.group(4));
            MonthDay dayMonth = MonthDay.of(Integer.parseInt(entry.group(2)), Integer.parseInt(entry.group(1)));
            LocalDate reference = section == Section.FUTURE ? nextClosing : closing;
            LocalDate date = PurchaseYearResolver.resolve(dayMonth, reference, split.number());
            RawLineKind kind;
            if (amount.isNegative()) {
                kind = RawLineKind.CREDIT;
            } else if (section == Section.PRODUCTS && split.number() == null) {
                kind = RawLineKind.FEE;
            } else {
                kind = RawLineKind.PURCHASE;
            }
            pending = new RawBillLine(0, kind, date, split.text(), split.number(), split.count(), amount, holder, null,
                    null, null, null);
            pendingNote.setLength(0);
            pendingHasDetail = false;
        }

        private void detail(String line) {
            if (section == Section.PURCHASES && !pendingHasDetail) {
                // RF-64b: "<Itaú category> <city>"; the category is kept as a suggestion
                String category = line.split(" ", 2)[0];
                pending = new RawBillLine(0, pending.kind(), pending.purchaseDate(), pending.description(),
                        pending.installmentNumber(), pending.installmentCount(), pending.amount(), pending.holderName(),
                        null, category.toLowerCase(Locale.ROOT), null, null);
                pendingHasDetail = true;
                return;
            }
            if (section == Section.INTERNATIONAL || section == Section.PRODUCTS) {
                // RF-64d dollar amount and rate, RF-64e PIX principal and interest
                if (!pendingNote.isEmpty()) {
                    pendingNote.append("; ");
                }
                pendingNote.append(line);
            }
        }

        private void flush() {
            if (pending == null) {
                return;
            }
            String note = pendingNote.isEmpty() ? pending.note() : pendingNote.toString();
            RawBillLine line = new RawBillLine(++sequence, pending.kind(), pending.purchaseDate(), pending.description(),
                    pending.installmentNumber(), pending.installmentCount(), pending.amount(), pending.holderName(), null,
                    pending.bankCategory(), note, null);
            pending = null;
            pendingNote.setLength(0);
            if (section == Section.FUTURE) {
                future.add(line);
            } else {
                add(line);
            }
        }

        private void add(RawBillLine line) {
            entries.add(line);
            switch (section) {
                case INTERNATIONAL -> internationalSum = internationalSum.plus(line.amount());
                case PRODUCTS -> productsSum = productsSum.plus(line.amount());
                default -> purchasesSum = purchasesSum.plus(line.amount());
            }
        }

        void finish() {
            flush();
        }
    }
}
