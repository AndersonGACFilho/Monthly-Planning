package com.planejamentomensal.shared.infra.storage;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds file names that are valid and unambiguous on Windows, Linux and exFAT drives.
 *
 * <p>Implements POR-04: generated names use only letters, digits, {@code -} and {@code _}; accents are
 * removed, everything is lower case (so two names never differ only in case) and Windows reserved names
 * are avoided.</p>
 *
 * @since 1.0
 */
public final class FileNameSanitizer {

    /** Longest base name kept, well below the 255-character limit of exFAT and NTFS. */
    public static final int MAX_BASE_LENGTH = 120;

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NOT_ALLOWED = Pattern.compile("[^a-z0-9_-]+");
    private static final Pattern REPEATED_DASHES = Pattern.compile("-{2,}");
    private static final Set<String> WINDOWS_RESERVED = Set.of(
            "con", "prn", "aux", "nul",
            "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    private FileNameSanitizer() {
    }

    /**
     * Turns free text into a safe base name.
     *
     * @param text any text, such as {@code "Extrato Mãe 11/2026"}; must not be {@code null}
     * @return the safe base name, such as {@code "extrato-mae-11-2026"}; {@code "file"} when nothing is left
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public static String baseName(String text) {
        Objects.requireNonNull(text, "text");
        String ascii = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        String name = NOT_ALLOWED.matcher(ascii.toLowerCase(Locale.ROOT)).replaceAll("-");
        name = REPEATED_DASHES.matcher(name).replaceAll("-");
        name = strip(name);
        if (name.length() > MAX_BASE_LENGTH) {
            name = strip(name.substring(0, MAX_BASE_LENGTH));
        }
        if (name.isEmpty()) {
            return "file";
        }
        return WINDOWS_RESERVED.contains(name) ? name + "_" : name;
    }

    /**
     * Builds a safe file name with an extension.
     *
     * @param text      free text for the base name
     * @param extension extension without the dot, such as {@code "pdf"}; only letters and digits are kept
     * @return the safe file name, such as {@code "extrato-mae-11-2026.pdf"}
     * @throws NullPointerException     if an argument is {@code null}
     * @throws IllegalArgumentException if the extension has no letter or digit
     */
    public static String fileName(String text, String extension) {
        String ext = Objects.requireNonNull(extension, "extension").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (ext.isEmpty()) {
            throw new IllegalArgumentException("Extension has no letter or digit: " + extension);
        }
        return baseName(text) + "." + ext;
    }

    private static String strip(String name) {
        int start = 0;
        int end = name.length();
        while (start < end && (name.charAt(start) == '-' || name.charAt(start) == '_')) {
            start++;
        }
        while (end > start && (name.charAt(end - 1) == '-' || name.charAt(end - 1) == '_')) {
            end--;
        }
        return name.substring(start, end);
    }
}
