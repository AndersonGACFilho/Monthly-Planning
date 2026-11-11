package com.planejamentomensal.shared.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * An error of the interface to write to the backend log (RNF-11).
 *
 * @param message   error message
 * @param detail    stack trace or extra detail, optional
 * @param url       screen where it happened, optional
 * @param requestId request id of the failed call, optional
 * @since 1.0
 */
public record ClientErrorRequest(
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 20000) String detail,
        @Size(max = 500) String url,
        @Size(max = 64) String requestId) {
}
