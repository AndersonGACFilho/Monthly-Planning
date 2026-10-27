package com.planejamentomensal.statement.application.workbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Drops the shared strings no cell uses any more. POI keeps every string a model ever had, so without this the
 * texts of the model's old rows would travel inside every exported file.
 *
 * @since 1.0
 */
final class SharedStringsCompactor {

    private static final String SST = "xl/sharedStrings.xml";
    private static final Pattern ITEM = Pattern.compile("<si>.*?</si>|<si/>", Pattern.DOTALL);
    private static final Pattern STRING_CELL = Pattern.compile("(<c [^>]*t=\"s\"[^>]*>(?:<f[^>]*/>|<f[^>]*>.*?</f>)?<v>)(\\d+)(</v>)",
            Pattern.DOTALL);

    private SharedStringsCompactor() {
    }

    /**
     * Rewrites a saved XLSX with only the used shared strings.
     *
     * @param xlsx file bytes
     * @return compacted file bytes
     */
    static byte[] compact(byte[] xlsx) {
        try {
            Map<String, byte[]> entries = new LinkedHashMap<>();
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    entries.put(entry.getName(), zip.readAllBytes());
                }
            }
            byte[] sstBytes = entries.get(SST);
            if (sstBytes == null) {
                return xlsx;
            }
            String sst = new String(sstBytes, StandardCharsets.UTF_8);
            List<String> items = new ArrayList<>();
            Matcher m = ITEM.matcher(sst);
            while (m.find()) {
                items.add(m.group());
            }
            Map<Integer, Integer> remap = new HashMap<>();
            List<String> kept = new ArrayList<>();
            int references = 0;
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                if (!e.getKey().startsWith("xl/worksheets/sheet")) {
                    continue;
                }
                String xml = new String(e.getValue(), StandardCharsets.UTF_8);
                Matcher cell = STRING_CELL.matcher(xml);
                StringBuilder out = new StringBuilder();
                while (cell.find()) {
                    int old = Integer.parseInt(cell.group(2));
                    int index = remap.computeIfAbsent(old, k -> {
                        kept.add(k < items.size() ? items.get(k) : "<si><t></t></si>");
                        return kept.size() - 1;
                    });
                    references++;
                    cell.appendReplacement(out, Matcher.quoteReplacement(cell.group(1) + index + cell.group(3)));
                }
                cell.appendTail(out);
                e.setValue(out.toString().getBytes(StandardCharsets.UTF_8));
            }
            int start = sst.indexOf("<sst");
            String head = sst.substring(0, sst.indexOf('>', start) + 1)
                    .replaceAll("count=\"\\d+\"", "count=\"" + references + "\"")
                    .replaceAll("uniqueCount=\"\\d+\"", "uniqueCount=\"" + kept.size() + "\"");
            entries.put(SST, (head + String.join("", kept) + "</sst>").getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                    zip.putNextEntry(new ZipEntry(e.getKey()));
                    zip.write(e.getValue());
                    zip.closeEntry();
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
