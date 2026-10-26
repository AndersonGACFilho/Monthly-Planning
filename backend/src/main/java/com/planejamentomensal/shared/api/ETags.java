package com.planejamentomensal.shared.api;

import com.planejamentomensal.shared.domain.StaleVersionException;
import org.springframework.http.ResponseEntity;

/**
 * Optimistic concurrency over HTTP: {@code ETag} from the JPA version and {@code If-Match} parsing (API-04).
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
     * Reads the version named by an {@code If-Match} header.
     *
     * @param ifMatch header value, possibly {@code null}, {@code *} or weak ({@code W/"3"})
     * @return the version, or {@code null} when nothing must be checked
     * @throws StaleVersionException if the header is not a version of this API
     */
    public static Long expectedVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank() || ifMatch.trim().equals("*")) {
            return null;
        }
        String value = ifMatch.trim().replaceFirst("^W/", "").replace("\"", "");
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new StaleVersionException(-1, -1);
        }
    }
}
