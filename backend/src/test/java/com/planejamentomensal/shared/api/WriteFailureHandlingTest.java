package com.planejamentomensal.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystemException;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

class WriteFailureHandlingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(messages());

    private static ResourceBundleMessageSource messages() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }

    @Test
    @DisplayName("POR-13: a removed or full drive answers 503 with a clear localized warning")
    void storageFailure() {
        ProblemDetail removed = handler.handleStorage(new DataAccessResourceFailureException("write",
                new SQLiteException("disk I/O error", SQLiteErrorCode.SQLITE_IOERR)), Locale.forLanguageTag("pt-BR"));
        assertThat(removed.getStatus()).isEqualTo(503);
        assertThat(removed.getProperties()).containsEntry("code", "storage-unavailable");
        assertThat(removed.getDetail()).contains("SSD").contains("feche o app");
        ProblemDetail full = handler.handleStorage(new UncheckedIOException(new IOException("No space left on device")), Locale.US);
        assertThat(full.getStatus()).isEqualTo(503);
        assertThat(full.getDetail()).contains("drive");
        assertThat(StorageFailures.isStorageFailure(new UncheckedIOException(new FileSystemException("/data")))).isTrue();
    }

    @Test
    @DisplayName("POR-13: other database errors stay internal errors")
    void otherFailure() {
        ProblemDetail other = handler.handleStorage(new DataIntegrityViolationException("unique"), Locale.US);
        assertThat(other.getStatus()).isEqualTo(500);
        assertThat(other.getProperties()).containsEntry("code", "internal-error");
        assertThat(StorageFailures.isStorageFailure(new UncheckedIOException(new IOException("other")))).isFalse();
    }
}
