package com.planejamentomensal.shared.api;

import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.shared.domain.BusinessRuleException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GlobalExceptionHandlerIT.ProbeController.class)
class GlobalExceptionHandlerIT {

    @Autowired
    private MockMvc mockMvc;

    record Probe(@NotBlank String name) {
    }

    @TestConfiguration
    @RestController
    static class ProbeController {

        @PostMapping("/api/test-probes")
        void create(@Valid @RequestBody Probe probe) {
            throw new BusinessRuleException("not-found", "probe", probe.name());
        }
    }

    @Test
    @DisplayName("API-03: missing resource is a 404 ProblemDetail")
    void notFoundIsProblem() throws Exception {
        mockMvc.perform(authorized(get("/api/profile/unknown")).header(HttpHeaders.ACCEPT_LANGUAGE, "en-US"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.detail").value("No relation with id unknown."))
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    @DisplayName("I18N-06: problem texts follow Accept-Language (pt-BR)")
    void problemInPortuguese() throws Exception {
        mockMvc.perform(authorized(get("/api/profile/unknown")).header(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Não encontrado"))
                .andExpect(jsonPath("$.detail").value("Não existe relation com id unknown."));
    }

    @Test
    @DisplayName("API-03: business rule conflict is a 409 ProblemDetail")
    void businessRuleIsConflict() throws Exception {
        mockMvc.perform(authorized(post("/api/test-probes"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("not-found"));
    }

    @Test
    @DisplayName("API-03: validation error is a 400 ProblemDetail")
    void validationIsBadRequest() throws Exception {
        mockMvc.perform(authorized(post("/api/test-probes"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("I18N-06: validation problem and field messages follow Accept-Language")
    void validationMessagesAreLocalized() throws Exception {
        mockMvc.perform(authorized(post("/api/test-probes")).header(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("não deve estar em branco"));
        mockMvc.perform(authorized(post("/api/test-probes")).header(HttpHeaders.ACCEPT_LANGUAGE, "en-US")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(jsonPath("$.title").value("Invalid data"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
    }

    @Test
    @DisplayName("I18N-06: unsupported language falls back to pt-BR")
    void unsupportedLanguageFallsBack() throws Exception {
        mockMvc.perform(authorized(get("/api/profile/unknown")).header(HttpHeaders.ACCEPT_LANGUAGE, "de-DE"))
                .andExpect(jsonPath("$.title").value("Não encontrado"));
    }
}
