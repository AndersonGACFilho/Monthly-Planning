package com.planejamentomensal.shared.api;

import com.planejamentomensal.shared.api.web.SessionTokenFilter;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

/**
 * Helpers shared by API tests (TST-05).
 */
public final class ApiTestSupport {

    /** Token set for the test JVM through {@code APP_SESSION_TOKEN} in the surefire configuration. */
    public static final String TOKEN = "test-session-token";

    private ApiTestSupport() {
    }

    /**
     * Adds the session token header to a request.
     *
     * @param request request builder, plain or multipart
     * @param <B>     builder type
     * @return the same builder with the token
     */
    public static <B extends AbstractMockHttpServletRequestBuilder<B>> B authorized(B request) {
        return request.header(SessionTokenFilter.HEADER, TOKEN);
    }
}
