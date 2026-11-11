package com.planejamentomensal.shared.api.web;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RequestIdFilterIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("RNF-11: every response carries a request id, new for each request")
    void newId() throws Exception {
        String first = mockMvc.perform(authorized(get("/api"))).andReturn().getResponse().getHeader(RequestIdFilter.HEADER);
        String second = mockMvc.perform(authorized(get("/api"))).andReturn().getResponse().getHeader(RequestIdFilter.HEADER);
        assertThat(first).matches("[a-f0-9]{12}");
        assertThat(second).matches("[a-f0-9]{12}").isNotEqualTo(first);
    }

    @Test
    @DisplayName("RNF-11: a request id sent by the interface is kept; an invalid one is replaced")
    void keepsValidId() throws Exception {
        mockMvc.perform(authorized(get("/api")).header(RequestIdFilter.HEADER, "front-42"))
                .andExpect(header().string(RequestIdFilter.HEADER, "front-42"));
        String replaced = mockMvc.perform(authorized(get("/api")).header(RequestIdFilter.HEADER, "bad id\n"))
                .andReturn().getResponse().getHeader(RequestIdFilter.HEADER);
        assertThat(replaced).matches("[a-f0-9]{12}");
    }

    @Test
    @DisplayName("RNF-11: the interface can read the request id across origins")
    void exposedThroughCors() throws Exception {
        mockMvc.perform(authorized(get("/api/settings")).header(HttpHeaders.ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString(RequestIdFilter.HEADER)));
    }
}
