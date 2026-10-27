package com.planejamentomensal.imports.application;

import com.planejamentomensal.imports.domain.BillFormat;
import com.planejamentomensal.imports.domain.ParsedBill;
import com.planejamentomensal.shared.domain.InvalidInputException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Registry of bill parsers by format; detects the format from the file name (RF-61, SOL-02).
 *
 * @since 1.0
 */
@Service
public class BillParsers {

    private final Map<BillFormat, BillParser> parsers = new EnumMap<>(BillFormat.class);

    /**
     * Creates the registry.
     *
     * @param all every parser bean
     */
    public BillParsers(List<BillParser> all) {
        all.forEach(parser -> parsers.put(parser.format(), parser));
    }

    /**
     * Detects the format by extension and reads the file.
     *
     * @param fileName uploaded file name
     * @param content  file bytes
     * @return what the file contains
     * @throws InvalidInputException with code {@code unsupported-bill-format} for other extensions, or
     *                               {@code invalid-bill-file} for unrecognized content
     */
    public ParsedBill parse(String fileName, byte[] content) {
        BillFormat format = BillFormat.fromFileName(fileName)
                .orElseThrow(() -> new InvalidInputException("unsupported-bill-format", fileName));
        BillParser parser = parsers.get(format);
        if (parser == null) {
            throw new InvalidInputException("unsupported-bill-format", fileName);
        }
        return parser.parse(content);
    }
}
