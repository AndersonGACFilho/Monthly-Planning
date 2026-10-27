package com.planejamentomensal.statement.application.workbook;

import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.statement.application.StatementFiles;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Loads, checks and repairs the spreadsheet model whose look the export copies (XLS-12 to XLS-14). The model keeps
 * the sheet names of the original Portuguese spreadsheet; the export renames them for other languages.
 *
 * @since 1.0
 */
@Component
public class WorkbookTemplate {

    /** Sheet names the model must have. */
    public static final List<String> SHEETS = List.of(ModelSheets.SUMMARY, ModelSheets.INCOME, ModelSheets.EXPENSES,
            ModelSheets.CARDS, ModelSheets.LISTS);
    /** Table names the model must have (XLS-08). */
    public static final List<String> TABLES = List.of(ModelSheets.INCOME_TABLE, ModelSheets.EXPENSES_TABLE,
            ModelSheets.CARDS_TABLE, ModelSheets.CATEGORIES_TABLE);

    private static final Pattern DETAIL_SHEET = Pattern.compile("(?i)detalhes\\d*|details\\d*");

    private final StatementFiles files;

    /**
     * Creates the template.
     *
     * @param files model file
     */
    public WorkbookTemplate(StatementFiles files) {
        this.files = files;
    }

    /**
     * Opens the model, ready to fill.
     *
     * @return the repaired model
     * @throws BusinessRuleException with code {@code workbook-model-invalid} listing what is missing (XLS-14)
     */
    public XSSFWorkbook open() {
        try {
            return open(Files.readAllBytes(files.workbookModel()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Checks and stores a model chosen by the user (XLS-14).
     *
     * @param model XLSX bytes
     * @throws BusinessRuleException with code {@code workbook-model-invalid} listing what is missing
     */
    public void replace(byte[] model) {
        try (XSSFWorkbook checked = open(model)) {
            files.replaceWorkbookModel(model);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Goes back to the bundled model.
     */
    public void reset() {
        files.replaceWorkbookModel(null);
    }

    /**
     * Returns the model in use.
     *
     * @return XLSX bytes
     */
    public byte[] current() {
        try {
            return Files.readAllBytes(files.workbookModel());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Opens a model from bytes: cleans the package, checks it and repairs it.
     *
     * @param model XLSX bytes
     * @return the repaired model
     * @throws BusinessRuleException with code {@code workbook-model-invalid} listing what is missing (XLS-14)
     */
    public static XSSFWorkbook open(byte[] model) {
        try {
            XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(ModelPackageCleaner.clean(model)));
            List<String> missing = missing(workbook);
            if (!missing.isEmpty()) {
                workbook.close();
                throw new BusinessRuleException("workbook-model-invalid", String.join(", ", missing));
            }
            repair(workbook);
            return workbook;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Lists the sheets and tables the model lacks (XLS-14).
     *
     * @param workbook model
     * @return missing names, empty when the model is usable
     */
    public static List<String> missing(XSSFWorkbook workbook) {
        List<String> missing = new ArrayList<>();
        SHEETS.stream().filter(name -> workbook.getSheet(name) == null).forEach(missing::add);
        TABLES.stream().filter(name -> Cells.table(workbook, name) == null).forEach(missing::add);
        return missing;
    }

    /**
     * Repairs the model (XLS-12): drops the sheets Excel creates when a pivot table is clicked, names that point to
     * {@code #REF!}, and every formula of the fixed sheets, which the export writes again.
     *
     * @param workbook model
     */
    static void repair(XSSFWorkbook workbook) {
        for (int i = workbook.getNumberOfSheets() - 1; i >= 0; i--) {
            if (DETAIL_SHEET.matcher(workbook.getSheetName(i)).matches()) {
                workbook.removeSheetAt(i);
            }
        }
        for (XSSFName name : new ArrayList<>(workbook.getAllNames())) {
            String formula = name.getRefersToFormula();
            if (formula == null || formula.contains("#REF!") || name.getSheetIndex() >= 0 && !name.getNameName().startsWith("_xlnm")) {
                workbook.removeName(name);
            }
        }
        for (String sheetName : SHEETS) {
            XSSFSheet sheet = workbook.getSheet(sheetName);
            for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                XSSFRow row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                for (var any : row) {
                    XSSFCell cell = (XSSFCell) any;
                    if (cell.isPartOfArrayFormulaGroup()) {
                        sheet.removeArrayFormula(cell);
                    } else if (cell.getCellType() == CellType.FORMULA) {
                        cell.setBlank();
                    }
                }
            }
        }
    }
}
