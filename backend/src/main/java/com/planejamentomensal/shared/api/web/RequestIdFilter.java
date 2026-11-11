package com.planejamentomensal.shared.api.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id, so an error the user sees can be found in the log (RNF-11).
 *
 * <p>The id comes from the interface when it sends a valid one, otherwise it is created here. It is put in the
 * logging context (every log line of the request shows it), in the response header and, through
 * {@code GlobalExceptionHandler}, in every problem.</p>
 *
 * @since 1.0
 */
public class RequestIdFilter extends OncePerRequestFilter {

    /** Request and response header that carries the id. */
    public static final String HEADER = "X-Request-Id";

    /** Key of the id in the logging context. */
    public static final String MDC_KEY = "requestId";

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9-]{1,64}");
    private static final int GENERATED_LENGTH = 12;

    /**
     * Creates the filter.
     */
    public RequestIdFilter() {
        // Stateless: the id lives in the logging context of each request
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = idOf(request.getHeader(HEADER));
        MDC.put(MDC_KEY, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static String idOf(String sent) {
        if (sent != null && VALID.matcher(sent).matches()) {
            return sent;
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, GENERATED_LENGTH);
    }
}
