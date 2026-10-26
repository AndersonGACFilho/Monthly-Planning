# Masked Itaú fixtures

Test copies of the samples received on 2026-10-26 (SPEC §8). The originals stay outside the repository and must never be committed.

| File | Source | Used by |
| --- | --- | --- |
| `itau-closed-bill-2026-10.pdf` | Closed bill (PDF), due 09/10/2026, issued 28/09/2026, next closing forecast 02/11/2026, "Total desta fatura" R$ 6.514,13, card final 4102 | `ItauPdfParserTest` (RF-64…RF-64h) |
| `itau-open-bill-2026-11.xlsx` | Open bill exported from the app ("Fatura Aberta - Novembro/2026"), finals 4102 / 4090 / 9103 | `ItauXlsxParserTest` (RF-63…RF-63f) |
| `monthly-workbook-model-2026-11.xlsx` | Monthly spreadsheet model "2026 - 11 - NOVEMBRO CJ" | `MonthlyWorkbookExporterTest`, `WorkbookTemplate` validation (XLS-*) |

## What was masked

The same fictitious values are used in all three files, so cross-format tests (RN-19, RN-21) still match:

- Cardholder name, full and abbreviated: `MARIANA SOUZA FERREIRA SILVA` / `MARIANA S F SILVA` (`Mariana S F Silva` in the XLSX).
- CPF: `123.456.789-09`.
- Address: `RUA EXEMPLO, 100` / `CASA 1 BAIRRO MODELO` / `01001-000 SAO PAULO SP`.
- Branch and account (XLSX header): `0001` / `12345-6`.
- Personal names inside merchant descriptors, and the people's names in the spreadsheet model (items, incomes, cards, categories and pivot cache). The model's `MÃE` sheet keeps its name, because it is a role, not a name (XLS-06).

Unchanged on purpose: merchants, amounts, dates, installments, card finals, the boleto barcode and digitable line, and file metadata. None of the metadata contained personal names.

## How it was done

- **XLSX:** only the text of `sharedStrings.xml` (and the pivot cache / one sheet in the model) was edited in place. Every other zip entry is byte-identical to the original, so the header search, the named tables and the formulas are untouched.
- **PDF:** each personal string was removed with a true redaction (the text is gone from the content stream) and redrawn in Helvetica at the same position and baseline. The font is scaled down so it is never wider than the original. All other words keep their exact coordinates, so the two-column extraction by area (RF-64a) behaves as in the original. Parsers must not depend on the font of the masked header and boleto lines.

## Verified

- No real name, CPF, address, branch or account appears in the extracted text, content streams, metadata or any XML part.
- PDF text with the mapping applied is identical to the masked PDF text, and every unmasked word keeps its coordinates.
- Both XLSX files open in LibreOffice headless without repair.
