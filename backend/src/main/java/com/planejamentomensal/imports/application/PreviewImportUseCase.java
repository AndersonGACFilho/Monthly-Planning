package com.planejamentomensal.imports.application;

import com.planejamentomensal.entry.application.EntryKeys;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.StoredEntry;
import com.planejamentomensal.imports.domain.BatchStatus;
import com.planejamentomensal.imports.domain.Classification;
import com.planejamentomensal.imports.domain.ExistingEntry;
import com.planejamentomensal.imports.domain.ImportBatch;
import com.planejamentomensal.imports.domain.ImportClassifier;
import com.planejamentomensal.imports.domain.ImportLine;
import com.planejamentomensal.imports.domain.ParsedBill;
import com.planejamentomensal.imports.domain.RawBillLine;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.PurchaseFacts;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads a bill, classifies each line and stores the preview, without writing any entry (RF-68).
 *
 * <p>Orchestrates parse → normalize → classify → preview (SPEC section 13). Implements RF-60 (card and
 * period chosen by the user win, RN-03), RF-62 (warn when the file's due month or card differ), RF-64h and
 * RF-71 (totals), RF-66 (members and finals), RF-69 (default decisions) and RF-73 (a file already imported
 * is flagged; its lines come out as duplicates). The file itself is not kept (RNF-04).</p>
 *
 * @since 1.0
 */
@Service
public class PreviewImportUseCase {

    /** Warning: the file's due month differs from the chosen period (RF-62). */
    public static final String PERIOD_MISMATCH = "period-mismatch";
    /** Warning: the file's card final is not one of the chosen card's finals (RF-62). */
    public static final String CARD_MISMATCH = "card-mismatch";
    /** Warning: this exact file was already imported (RF-73). */
    public static final String ALREADY_IMPORTED = "already-imported";
    /** Warning: printed totals do not match the lines read (RF-64h). */
    public static final String TOTALS_MISMATCH = "totals-mismatch";

    private final BillParsers parsers;
    private final CardResolver resolver;
    private final EntryLedger ledger;
    private final ImportBatchRepository batches;
    private final ImportLineRepository lines;
    private final List<PreviewEnricher> enrichers;

    /**
     * Creates the use case.
     *
     * @param parsers   parsers by format
     * @param resolver  holder and final mapping
     * @param ledger    entries of the card
     * @param batches   batch storage
     * @param lines     preview line storage
     * @param enrichers suggestions added to preview lines, such as aliases (RF-70)
     */
    public PreviewImportUseCase(BillParsers parsers, CardResolver resolver, EntryLedger ledger,
            ImportBatchRepository batches, ImportLineRepository lines, List<PreviewEnricher> enrichers) {
        this.parsers = parsers;
        this.resolver = resolver;
        this.ledger = ledger;
        this.batches = batches;
        this.lines = lines;
        this.enrichers = enrichers;
    }

    /**
     * Previews an import.
     *
     * @param cardId        card chosen by the user
     * @param billingPeriod period chosen by the user
     * @param fileName      uploaded file name
     * @param content       file bytes, kept only in memory
     * @return the batch in preview
     */
    @Transactional
    public ImportBatch preview(long cardId, BillingPeriod billingPeriod, String fileName, byte[] content) {
        ParsedBill bill = parsers.parse(fileName, content);
        CardResolver.CardGroup group = resolver.group(cardId);
        long billingCardId = group.billingCard().id();
        String hash = sha256(content);

        List<String> warnings = new ArrayList<>();
        if (!bill.billingPeriod().equals(billingPeriod)) {
            warnings.add(PERIOD_MISMATCH);
        }
        if (bill.cardLastDigits() != null && !group.hasFinal(bill.cardLastDigits())) {
            warnings.add(CARD_MISMATCH);
        }
        if (!batches.findByFileHashAndStatus(hash, BatchStatus.CONFIRMED).isEmpty()) {
            warnings.add(ALREADY_IMPORTED);
        }
        if (!bill.totalsMatch()) {
            warnings.add(TOTALS_MISMATCH);
        }
        ImportBatch batch = batches.save(new ImportBatch(billingCardId, billingPeriod, fileName, hash, bill, warnings));

        ImportClassifier classifier = new ImportClassifier(ledger.entriesForMatching(billingCardId).stream()
                .map(PreviewImportUseCase::existing).toList());
        Map<String, Integer> occurrences = new HashMap<>();
        List<ImportLine> preview = new ArrayList<>();
        for (RawBillLine line : bill.lines()) {
            preview.add(line(batch, line, false, billingCardId, billingPeriod, classifier, occurrences, group));
        }
        for (RawBillLine line : bill.futureInstallments()) {
            preview.add(line(batch, line, true, billingCardId, billingPeriod, null, occurrences, group));
        }
        enrichers.forEach(enricher -> enricher.enrich(preview));
        lines.saveAll(preview);
        batch.count(preview);
        return batches.save(batch);
    }

    private ImportLine line(ImportBatch batch, RawBillLine line, boolean future, long cardId, BillingPeriod period,
            ImportClassifier classifier, Map<String, Integer> occurrences, CardResolver.CardGroup group) {
        EntryKeys.Keys base = EntryKeys.of(cardId, line.purchaseDate(), line.description(), line.amount(),
                line.installmentNumber(), line.installmentCount(), 1);
        String occurrenceKey = (future ? "F" : "L") + base.installmentKey();
        int occurrence = occurrences.merge(occurrenceKey, 1, Integer::sum);
        EntryKeys.Keys keys = EntryKeys.of(cardId, line.purchaseDate(), line.description(), line.amount(),
                line.installmentNumber(), line.installmentCount(), occurrence);
        Classification classification = classifier == null ? Classification.newLine()
                : classifier.classify(new PurchaseFacts(cardId, line.purchaseDate(), line.description(),
                        line.installmentNumber(), line.installmentCount(), line.amount()), keys.installmentKey(), period);
        CardResolver.Resolution resolution = resolver.resolve(group, line.cardLastDigits(), line.holderName());
        return new ImportLine(batch.id(), line, future, occurrence, keys.purchaseKey(), keys.installmentKey(),
                classification, resolution.memberId(), resolution.unknownFinal());
    }

    private static ExistingEntry existing(StoredEntry stored) {
        return new ExistingEntry(stored.entryId(), stored.installmentKey(), ExistingEntry.State.valueOf(stored.status()),
                stored.facts(), stored.period(), stored.subscription());
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
