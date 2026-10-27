package com.planejamentomensal.shared.api;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.Locale;
import java.util.Set;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

/**
 * Tells failures of the storage itself (drive removed, full or read-only) apart from other errors (POR-13).
 *
 * @since 1.0
 */
final class StorageFailures {

    private static final Set<SQLiteErrorCode> STORAGE_CODES = Set.of(SQLiteErrorCode.SQLITE_IOERR, SQLiteErrorCode.SQLITE_FULL,
            SQLiteErrorCode.SQLITE_CANTOPEN, SQLiteErrorCode.SQLITE_READONLY, SQLiteErrorCode.SQLITE_NOTADB,
            SQLiteErrorCode.SQLITE_CORRUPT);

    private StorageFailures() {
    }

    /**
     * Walks the causes looking for an I/O or SQLite storage error.
     *
     * @param error error
     * @return {@code true} if the storage failed
     */
    static boolean isStorageFailure(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLiteException sqlite && (STORAGE_CODES.contains(sqlite.getResultCode())
                    || sqlite.getResultCode().name().startsWith("SQLITE_IOERR"))) {
                return true;
            }
            if (t instanceof FileSystemException) {
                return true;
            }
            if (t instanceof IOException io && io.getMessage() != null) {
                String message = io.getMessage().toLowerCase(Locale.ROOT);
                if (message.contains("no space") || message.contains("read-only") || message.contains("no such device")
                        || message.contains("input/output error")) {
                    return true;
                }
            }
        }
        return false;
    }
}
