package com.planejamentomensal.category.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static com.planejamentomensal.shared.api.JsonBodies.read;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    private String create(String name, Long parent) throws Exception {
        return mockMvc.perform(authorized(post("/api/categories")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"parentId\":%s,\"color\":\"#16a34a\",\"icon\":\"shopping-cart\"}"
                                .formatted(name, parent)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);
    }

    private long id(String location) {
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    @Test
    @DisplayName("RF-07: \"not classified\" always exists, is marked as system and cannot be deleted")
    void unclassifiedExists() throws Exception {
        var result = mockMvc.perform(authorized(get("/api/categories")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.categories[?(@.system == true)].name", hasItem("Não classificado")))
                .andReturn();
        Number id = ((net.minidev.json.JSONArray) read(result, "$._embedded.categories[?(@.system == true)].id")).stream()
                .map(Number.class::cast).findFirst().orElseThrow();
        mockMvc.perform(authorized(get("/api/categories/" + id))).andExpect(jsonPath("$._links.delete").doesNotExist());
        mockMvc.perform(authorized(delete("/api/categories/" + id))).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("RF-06: categories have colour, icon and an optional single-level parent")
    void subcategories() throws Exception {
        String parent = create("Mercado", null);
        String child = create("Feira", id(parent));
        mockMvc.perform(authorized(get(child)))
                .andExpect(jsonPath("$.parentId").value(id(parent)))
                .andExpect(jsonPath("$.color").value("#16A34A"))
                .andExpect(jsonPath("$._links.parent.href").exists());
        mockMvc.perform(authorized(get(parent))).andExpect(jsonPath("$._links.delete").doesNotExist());
        mockMvc.perform(authorized(post("/api/categories")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Neta\",\"parentId\":%d,\"color\":\"#000000\",\"icon\":\"x\"}".formatted(id(child))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("category-parent-invalid"));
        mockMvc.perform(authorized(delete(child))).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("I18N-07: initial categories are created once, in the language chosen on first use")
    void defaults() throws Exception {
        mockMvc.perform(authorized(post("/api/categories/defaults")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"en-US\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.categories[*].name", hasItems("Groceries", "Health", "Subscriptions")));
        mockMvc.perform(authorized(post("/api/categories/defaults")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"pt-BR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded").doesNotExist());
    }
}
