package com.example.allowance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "app.security.username=alice", "app.security.password=test-password-123"})
@AutoConfigureMockMvc
class AllowanceApiTest {
    @Autowired MockMvc mvc;
    @Autowired AllowanceRepository repository;
    @Autowired ObjectMapper json;
    private static final String BODY = """
            {"cardholderName":"Alex","lastFour":"1234","monthlyLimit":250.00,"currency":"USD"}
            """;

    @BeforeEach void clear() { repository.deleteAll(); }

    private long create() throws Exception {
        var result = mvc.perform(post("/api/allowances").with(httpBasic("alice", "test-password-123"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.monthlyLimit").value(250.00)).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test void completeCrudLifecycle() throws Exception {
        long id = create();
        mvc.perform(get("/api/allowances/" + id).with(user("alice")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lastFour").value("1234"));
        mvc.perform(put("/api/allowances/" + id).with(user("alice"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("250.00", "300.50")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.monthlyLimit").value(300.50));
        mvc.perform(get("/api/allowances/" + id).with(user("alice")))
                .andExpect(jsonPath("$.monthlyLimit").value(300.50));
        mvc.perform(get("/api/allowances").with(user("alice")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(delete("/api/allowances/" + id).with(user("alice"))).andExpect(status().isNoContent());
        mvc.perform(get("/api/allowances/" + id).with(user("alice"))).andExpect(status().isNotFound());
    }

    @Test void requiresValidAuthentication() throws Exception {
        mvc.perform(get("/api/allowances")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/allowances").with(httpBasic("alice", "wrong"))).andExpect(status().isUnauthorized());
    }

    @Test void isolatesOwnersForAllOperations() throws Exception {
        long id = create();
        mvc.perform(get("/api/allowances/" + id).with(user("bob"))).andExpect(status().isNotFound());
        mvc.perform(put("/api/allowances/" + id).with(user("bob"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/allowances/" + id).with(user("bob"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/allowances").with(user("bob")))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/allowances/" + id).with(user("alice"))).andExpect(status().isOk());
    }

    @Test void rejectsInvalidFieldsAndUnknownFields() throws Exception {
        for (String body : new String[]{BODY.replace("250.00", "-1"), BODY.replace("250.00", "1.001"),
                BODY.replace("1234", "1234567812345678"), BODY.replace("Alex", " "),
                BODY.replace("USD", "XYZ"), BODY.replace("250.00", "10000000000"),
                BODY.replace("250.00", "null"), BODY.replace("{", "{\"owner\":\"bob\","), "{}"}) {
            mvc.perform(post("/api/allowances").with(user("alice"))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
    }

    @Test void boundsAndOrdersPagination() throws Exception {
        create(); create(); create();
        mvc.perform(get("/api/allowances?page=1&size=2").with(user("alice")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(2));
        for (String query : new String[]{"page=-1", "size=0", "size=101"}) {
            mvc.perform(get("/api/allowances?" + query).with(user("alice"))).andExpect(status().isBadRequest());
        }
    }

    @Test void missingRecordsReturn404() throws Exception {
        mvc.perform(get("/api/allowances/999999").with(user("alice"))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/allowances/999999").with(user("alice"))).andExpect(status().isNotFound());
    }
}
