package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.statement.domain.MemberStatement;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * The statement as a spreadsheet or CSV, for members who check it line by line (RF-84). Both list the same rows:
 * section, card, date, description, installment, category, share and amount.
 *
 * @since 1.0
 */
@Component
public class StatementSpreadsheet {

    private final MessageSource messages;

    /**
     * Creates the writer.
     *
     * @param messages column titles
     */
    public StatementSpreadsheet(MessageSource messages) {
        this.messages = messages;
    }

    /**
     * Writes an XLSX file.
     *
     * @param statement statement
     * @param language  language tag
     * @return XLSX bytes
     */
    public byte[] toXlsx(MemberStatement statement, String language) {
        StatementFormat format = StatementFormat.of(language);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(label("title", format.locale()));
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("\"R$\" #,##0.00"));
            CellStyle date = workbook.createCellStyle();
            date.setDataFormat(workbook.createDataFormat().getFormat(Locale.US.equals(format.locale()) ? "mm/dd/yyyy" : "dd/mm/yyyy"));
            int r = 0;
            Row title = sheet.createRow(r++);
            title.createCell(0).setCellValue(label("title", format.locale()) + " " + statement.memberCode() + " — "
                    + format.period(statement.billingPeriod()));
            r++;
            Row head = sheet.createRow(r++);
            List<String> headers = headers(format.locale());
            for (int i = 0; i < headers.size(); i++) {
                head.createCell(i).setCellValue(headers.get(i));
            }
            for (Line line : lines(statement, format)) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(line.section());
                row.createCell(1).setCellValue(line.card());
                if (line.date() != null) {
                    row.createCell(2).setCellValue(line.date());
                    row.getCell(2).setCellStyle(date);
                }
                row.createCell(3).setCellValue(line.description());
                row.createCell(4).setCellValue(line.installment());
                row.createCell(5).setCellValue(line.category());
                row.createCell(6).setCellValue(line.share());
                row.createCell(7).setCellValue(line.amount().amount().doubleValue());
                row.getCell(7).setCellStyle(money);
            }
            Row total = sheet.createRow(++r);
            total.createCell(6).setCellValue(label("total", format.locale()));
            total.createCell(7).setCellValue(statement.total().amount().doubleValue());
            total.getCell(7).setCellStyle(money);
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes a CSV file in UTF-8 with BOM, separated by semicolons in pt-BR and commas in en-US.
     *
     * @param statement statement
     * @param language  language tag
     * @return CSV bytes
     */
    public byte[] toCsv(MemberStatement statement, String language) {
        StatementFormat format = StatementFormat.of(language);
        char separator = Locale.US.equals(format.locale()) ? ',' : ';';
        StringBuilder csv = new StringBuilder("﻿");
        csv.append(String.join(String.valueOf(separator), headers(format.locale()).stream().map(StatementSpreadsheet::quote).toList()))
                .append("\r\n");
        for (Line line : lines(statement, format)) {
            List<String> cells = List.of(line.section(), line.card(), line.date() == null ? "" : format.date(line.date()),
                    line.description(), line.installment(), line.category(), line.share(), number(line.amount(), format));
            csv.append(String.join(String.valueOf(separator), cells.stream().map(StatementSpreadsheet::quote).toList()))
                    .append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<String> headers(Locale locale) {
        return List.of(label("summary", locale), label("cards", locale), label("date", locale), label("description", locale),
                label("installment", locale), label("category", locale), label("share", locale), label("amount", locale));
    }

    private List<Line> lines(MemberStatement s, StatementFormat format) {
        Locale locale = format.locale();
        List<Line> lines = new ArrayList<>();
        s.cards().forEach(g -> g.lines().forEach(l -> lines.add(new Line(label("cards", locale), g.cardName(), l.date(),
                l.description(), l.installment(), nullToEmpty(l.category()), format.percent(l.percent()), l.amount()))));
        s.fixedExpenses().forEach(e -> lines.add(new Line(label("fixedExpenses", locale), "", null, e.description(), "",
                nullToEmpty(e.category()), "", e.amount())));
        s.variableExpenses().forEach(e -> lines.add(new Line(label("variableExpenses", locale), "", e.date(), e.description(),
                "", nullToEmpty(e.category()), "", e.amount())));
        return lines;
    }

    private String label(String key, Locale locale) {
        return messages.getMessage("statement." + key, null, locale);
    }

    private static String number(Money amount, StatementFormat format) {
        String plain = amount.amount().toPlainString();
        return Locale.US.equals(format.locale()) ? plain : plain.replace('.', ',');
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Line(String section, String card, LocalDate date, String description, String installment,
            String category, String share, Money amount) {
    }
}
