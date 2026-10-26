package com.planejamentomensal.shared.api;

import com.planejamentomensal.shared.domain.StaleVersionException;
import org.springframework.http.ResponseEntity;

/**
 * Optimistic concurrency over HTTP: {@code ETag} from the JPA version and {@code If-Match} checks (API-04).
 *
 * @since 1.0
 */
public final class ETags {

    private ETags() {
    }

    /**
     * Formats a version as a strong entity tag.
     *
     * @param version JPA {@code @Version} value
     * @return such as {@code "3"} (with quotes)
     */
    public static String of(long version) {
        return "\"" + version + "\"";
    }

    /**
     * Builds a 200 response with the entity tag of a version.
     *
     * @param body    response body
     * @param version version of the resource
     * @param <T>     body type
     * @return response with {@code ETag}
     */
    public static <T> ResponseEntity<T> ok(T body, long version) {
        return ResponseEntity.ok().eTag(of(version)).body(body);
    }

    /**
     * Checks an {@code If-Match} header against the stored version.
     *
     * @param ifMatch header value, or {@code null} when the client sent none (then nothing is checked)
     * @param current stored version
     * @throws StaleVersionException if the header names another version
     */
    public static void checkIfMatch(String ifMatch, long current) {
        if (ifMatch == null || ifMatch.isBlank() || ifMatch.trim().equals("*")) {
            return;
        }
        String value = ifMatch.trim().replaceFirst("^W/", "").replace("\"", "");
        long expected;
        try {
            expected = Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new StaleVersionException(-1, current);
        }
        if (expected != current) {
            throw new StaleVersionException(expected, current);
        }
    }
}
