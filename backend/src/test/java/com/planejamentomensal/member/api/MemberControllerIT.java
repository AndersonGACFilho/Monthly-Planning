package com.planejamentomensal.member.api;

import static com.planejamentomensal.member.MemberTestData.memberJson;
import static com.planejamentomensal.shared.api.ApiTestSupport.authorized;
import static com.planejamentomensal.shared.api.JsonBodies.read;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planejamentomensal.member.application.MemberReferences;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(MemberControllerIT.References.class)
class MemberControllerIT {

    static final Set<Long> REFERENCED = new HashSet<>();

    @TestConfiguration
    static class References {
        @Bean
        MemberReferences testReferences() {
            return REFERENCED::contains;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void reset() {
        REFERENCED.clear();
    }

    private String create(String code, String kind) throws Exception {
        var result = mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson(code, "Name " + code, kind)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(header().string(HttpHeaders.ETAG, "\"0\""))
                .andReturn();
        return result.getResponse().getHeader(HttpHeaders.LOCATION);
    }

    @Test
    @DisplayName("RF-01: member is created with code, name, kind, colour and contacts")
    void createsMember() throws Exception {
        String location = create("AB", "MAIN");
        mockMvc.perform(authorized(get(location)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("AB"))
                .andExpect(jsonPath("$.kind").value("MAIN"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.color").value("#2563EB"))
                .andExpect(jsonPath("$.whatsappPhone").value("+5551999990000"))
                .andExpect(jsonPath("$.pixKey").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").value(location));
    }

    @Test
    @DisplayName("API-05: members list is paged with links")
    void listsPaged() throws Exception {
        create("ZZ", "MAIN");
        create("M", "THIRD_PARTY");
        mockMvc.perform(authorized(get("/api/members?size=1&sort=code,asc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.members.length()").value(1))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$._links.next.href").exists())
                .andExpect(jsonPath("$._links.first.href").exists());
    }

    @Test
    @DisplayName("RF-01: codes are unique")
    void uniqueCodes() throws Exception {
        create("T", "THIRD_PARTY");
        mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson("T", "Other", "MAIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member-code-taken"));
    }

    @Test
    @DisplayName("RF-01: invalid data is a 400 with field errors")
    void validation() throws Exception {
        mockMvc.perform(authorized(post("/api/members")).contentType(MediaType.APPLICATION_JSON)
                        .content(memberJson("abcde", "", "MAIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    @DisplayName("API-04: editing an outdated version is a 412")
    void staleEdit() throws Exception {
        String location = create("AP", "THIRD_PARTY");
        mockMvc.perform(authorized(put(location)).header(HttpHeaders.IF_MATCH, "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content(memberJson("AP", "Renamed", "THIRD_PARTY")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(jsonPath("$.name").value("Renamed"));
        mockMvc.perform(authorized(put(location)).header(HttpHeaders.IF_MATCH, "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content(memberJson("AP", "Again", "THIRD_PARTY")))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("stale-version"));
    }

    @Test
    @DisplayName("RF-01, API-09: member with entries cannot be deleted, only inactivated")
    void referencedMemberIsInactivated() throws Exception {
        String location = create("CA", "MAIN");
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        REFERENCED.add(id);
        mockMvc.perform(authorized(get(location)))
                .andExpect(jsonPath("$._links.delete").doesNotExist())
                .andExpect(jsonPath("$._links.inactivate.href").exists());
        mockMvc.perform(authorized(delete(location)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("member-in-use"));
        mockMvc.perform(authorized(patch(location)).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$._links.activate.href").exists())
                .andExpect(jsonPath("$._links.inactivate").doesNotExist());
    }

    @Test
    @DisplayName("RF-01, API-09: unreferenced member offers and accepts delete")
    void deletesUnreferenced() throws Exception {
        String location = create("DL", "MAIN");
        var result = mockMvc.perform(authorized(get(location))).andReturn();
        String deleteHref = read(result, "$._links.delete.href");
        mockMvc.perform(authorized(delete(deleteHref))).andExpect(status().isNoContent());
        mockMvc.perform(authorized(get(location))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("API-07: entry point links to members")
    void rootLinksMembers() throws Exception {
        mockMvc.perform(authorized(get("/api"))).andExpect(jsonPath("$._links.members.href").value("/api/members"));
    }
}
