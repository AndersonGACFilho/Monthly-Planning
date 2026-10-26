package com.planejamentomensal.shared.api;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Reads values from JSON responses in API tests (TST-05).
 */
public final class JsonBodies {

    private JsonBodies() {
    }

    /**
     * Reads one value.
     *
     * @param result MockMvc result
     * @param path   JSON path
     * @param <T>    value type
     * @return the value
     * @throws Exception if the body cannot be read
     */
    public static <T> T read(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }
}
