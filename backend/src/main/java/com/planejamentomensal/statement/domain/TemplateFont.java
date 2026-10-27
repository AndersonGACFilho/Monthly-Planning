package com.planejamentomensal.statement.domain;

/**
 * Fonts a template can use; all are embedded in the PDF (POR-05, TPL-02).
 *
 * @since 1.0
 */
public enum TemplateFont {
    /** Noto Sans. */
    NOTO_SANS("Noto Sans", "NotoSans"),
    /** Noto Serif. */
    NOTO_SERIF("Noto Serif", "NotoSerif"),
    /** Liberation Sans. */
    LIBERATION_SANS("Liberation Sans", "LiberationSans");

    private final String family;
    private final String filePrefix;

    TemplateFont(String family, String filePrefix) {
        this.family = family;
        this.filePrefix = filePrefix;
    }

    /**
     * Returns the CSS font family.
     *
     * @return family name
     */
    public String family() {
        return family;
    }

    /**
     * Returns the prefix of the font files, such as {@code NotoSans} for {@code NotoSans-Regular.ttf}.
     *
     * @return file prefix
     */
    public String filePrefix() {
        return filePrefix;
    }
}
