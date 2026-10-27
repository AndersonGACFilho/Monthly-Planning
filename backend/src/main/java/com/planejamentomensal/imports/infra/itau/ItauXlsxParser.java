package com.planejamentomensal.imports.infra.itau;

import com.planejamentomensal.imports.application.BillParser;
import com.planejamentomensal.imports.domain.BillFormat;
import com.planejamentomensal.imports.domain.ParsedBill;
import com.planejamentomensal.imports.domain.RawBillLine;
import com.planejamentomensal.imports.domain.RawLineKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.domain.Money;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

/**
 * Reads the open bill exported as XLSX from the Itaú app (RF-63 to RF-63e).
 *
 * <p>Everything is found by text, never by row number: the period in "Fatura Aberta - &lt;mês&gt;/&lt;ano&gt;",
 * the due date below "Vencimento", the header row where "Data" is followed by "Lançamento", and the
 * columns by header name. Lines are read until the first empty row; the subtotal, "Importante saber" and
 * legal text are ignored, and the total is computed, never read from the broken formula.</p>
 *
 * @since 1.0
 */
@Component
public class ItauXlsxParser implements BillParser {

    private static final Pattern TITLE = Pattern.compile("Fatura\\s+(Aberta|Fechada)\\s*-\\s*([\\p{L}]+)\\s*/\\s*(\\d{4})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INSTALLMENT = Pattern.compile("Parcela\\s+(\\d+)\\s+de\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FINAL = Pattern.compile("(\\d{4})\\s*$");
    private static final String IOF_PREFIX = "iof internacional - ";

    /** Header names read by the parser (RF-63a). */
    private enum Column {
        DATE("Data"), DESCRIPTION("Lançamento"), INSTALLMENT("Parcelamento"), AMOUNT("Valor"),
        HOLDER_KIND("Titularidade"), HOLDER("Nome"), CARD_KIND("Tipo do cartão"), CARD_NUMBER("Número do cartão");

        private final String header;

        Column(String header) {
            this.header = header;
        }
    }

    /**
     * Creates the parser.
     */
    public ItauXlsxParser() {
        // stateless
    }

    @Override
    public BillFormat format() {
        return BillFormat.XLSX;
    }

    @Override
    public ParsedBill parse(byte[] content) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
            return parse(workbook.getSheetAt(0));
        } catch (IOException | RuntimeException e) {
            if (e instanceof InvalidInputException invalid) {
                throw invalid;
            }
            throw new InvalidInputException("invalid-bill-file", "XLSX", e.getMessage());
        }
    }

    private ParsedBill parse(Sheet sheet) {
        BillingPeriod period = null;
        boolean open = true;
        LocalDate dueDate = null;
        String cardFinal = null;
        Money partialTotal = null;
        int headerRow = -1;
        Map<Column, Integer> columns = new HashMap<>();

        for (Row row : sheet) {
            for (Cell cell : row) {
                String text = text(cell);
                Matcher title = TITLE.matcher(text);
                if (period == null && title.find()) {
                    open = title.group(1).equalsIgnoreCase("Aberta");
                    period = ItauText.period(title.group(2), Integer.parseInt(title.group(3))).orElse(null);
                } else if (dueDate == null && text.equalsIgnoreCase("Vencimento")) {
                    dueDate = below(sheet, cell).map(this::date).orElse(null);
                } else if (cardFinal == null && text.equalsIgnoreCase("Cartão")) {
                    cardFinal = below(sheet, cell).map(this::text).map(FINAL::matcher)
                            .filter(Matcher::find).map(m -> m.group(1)).orElse(null);
                } else if (partialTotal == null && text.toLowerCase(Locale.ROOT).startsWith("valor (parcial)")) {
                    partialTotal = below(sheet, cell).map(this::amount).orElse(null);
                } else if (headerRow < 0 && text.equals("Data") && text(row.getCell(cell.getColumnIndex() + 1)).equals("Lançamento")) {
                    headerRow = row.getRowNum();
                    for (Cell header : row) {
                        for (Column column : Column.values()) {
                            if (text(header).equalsIgnoreCase(column.header)) {
                                columns.putIfAbsent(column, header.getColumnIndex());
                            }
                        }
                    }
                }
            }
        }
        if (period == null || dueDate == null || headerRow < 0 || !columns.keySet().containsAll(
                List.of(Column.DATE, Column.DESCRIPTION, Column.AMOUNT))) {
            throw new InvalidInputException("invalid-bill-file", "XLSX", "missing title, due date or header");
        }
        List<RawBillLine> lines = readLines(sheet, headerRow, columns);
        BillingPeriod billingPeriod = BillingPeriod.ofDueDate(dueDate).equals(period) ? period : BillingPeriod.ofDueDate(dueDate);
        return new ParsedBill(BillFormat.XLSX, open, billingPeriod, dueDate, null, null, cardFinal, partialTotal, null,
                List.of(), lines, List.of());
    }

    private List<RawBillLine> readLines(Sheet sheet, int headerRow, Map<Column, Integer> columns) {
        List<RawBillLine> lines = new ArrayList<>();
        int sequence = 0;
        for (int r = headerRow + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            String description = row == null ? "" : text(row.getCell(columns.get(Column.DESCRIPTION)));
            Cell dateCell = row == null ? null : row.getCell(columns.get(Column.DATE));
            if (description.isEmpty() && text(dateCell).isEmpty()) {
                break; // RF-63b: read until the first empty row
            }
            Money amount = amount(row.getCell(columns.get(Column.AMOUNT)));
            if (amount.isNegative() && description.toLowerCase(Locale.ROOT).startsWith("pagamento")) {
                continue; // RF-63d: payment of the previous bill
            }
            Integer number = null;
            Integer count = null;
            Matcher installment = INSTALLMENT.matcher(cell(row, columns, Column.INSTALLMENT));
            if (installment.find()) {
                number = Integer.parseInt(installment.group(1));
                count = Integer.parseInt(installment.group(2));
            }
            String lower = description.toLowerCase(Locale.ROOT);
            RawLineKind kind = amount.isNegative() ? RawLineKind.CREDIT
                    : lower.startsWith("mensalidade") || lower.startsWith("anuidade") ? RawLineKind.FEE
                    : RawLineKind.PURCHASE;
            String related = null;
            if (lower.startsWith(IOF_PREFIX)) {
                kind = RawLineKind.IOF;
                related = description.substring(IOF_PREFIX.length()).strip();
            }
            Matcher finalDigits = FINAL.matcher(cell(row, columns, Column.CARD_NUMBER));
            lines.add(new RawBillLine(++sequence, kind, date(dateCell), description, number, count, amount,
                    emptyToNull(cell(row, columns, Column.HOLDER)), finalDigits.find() ? finalDigits.group(1) : null,
                    null, null, related));
        }
        return lines;
    }

    private String cell(Row row, Map<Column, Integer> columns, Column column) {
        Integer index = columns.get(column);
        return index == null ? "" : text(row.getCell(index));
    }

    private static java.util.Optional<Cell> below(Sheet sheet, Cell cell) {
        Row next = sheet.getRow(cell.getRowIndex() + 1);
        return java.util.Optional.ofNullable(next == null ? null : next.getCell(cell.getColumnIndex()));
    }

    private String text(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().strip();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.STRING ? cell.getStringCellValue().strip() : "";
            default -> "";
        };
    }

    private LocalDate date(Cell cell) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String text = text(cell);
        return text.contains("/") ? ItauText.date(text) : LocalDate.parse(text);
    }

    private Money amount(Cell cell) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            return Money.of(BigDecimal.valueOf(cell.getNumericCellValue()));
        }
        return ItauText.money(text(cell));
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
