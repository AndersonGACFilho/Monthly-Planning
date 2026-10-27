package com.planejamentomensal.imports.infra.itau;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.InputStream;

/**
 * Masked Itaú fixtures (SPEC section 15, IMP-21).
 */
public final class Fixtures {

    /** Closed bill, due 09/10/2026. */
    public static final String CLOSED_PDF = "itau-closed-bill-2026-10.pdf";
    /** Open bill, due 09/11/2026. */
    public static final String OPEN_XLSX = "itau-open-bill-2026-11.xlsx";

    private Fixtures() {
    }

    /**
     * Reads a fixture.
     *
     * @param name file name in {@code fixtures/itau}
     * @return file bytes
     */
    public static byte[] bytes(String name) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/fixtures/itau/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing fixture " + name);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
