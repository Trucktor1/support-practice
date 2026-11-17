package ru.practice.support;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Tag("integration")
class ApiTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.6-alpine");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @TestConfiguration
  static class Time {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.execute("TRUNCATE status_history,tickets");
  }

  String create() throws Exception {
    String body =
        mvc.perform(
                post("/api/tickets")
                    .contentType("application/json")
                    .content(
                        """
 {"title":"Printer","description":"Offline","categoryId":1,"priority":"HIGH","dueAt":"2025-12-31T00:00:00Z"}
 """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("NEW"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return mapper.readTree(body).get("id").asText();
  }

  void change(String id, String state, int code) throws Exception {
    mvc.perform(
            patch("/api/tickets/" + id + "/status")
                .contentType("application/json")
                .content(mapper.writeValueAsString(Map.of("status", state))))
        .andExpect(status().is(code));
  }

  @Test
  void migrationAndCategories() throws Exception {
    mvc.perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE success=true", Integer.class));
  }

  @Test
  void creationAndHistory() throws Exception {
    String id = create();
    mvc.perform(get("/api/tickets/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Printer"));
    mvc.perform(get("/api/tickets/" + id + "/history")).andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void validation400() throws Exception {
    mvc.perform(post("/api/tickets").contentType("application/json").content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void unknown404() throws Exception {
    mvc.perform(get("/api/tickets/" + UUID.randomUUID())).andExpect(status().isNotFound());
  }

  @Test
  void filterAndPage() throws Exception {
    create();
    create();
    mvc.perform(
            get("/api/tickets?status=NEW&priority=HIGH&categoryId=1&overdue=true&size=1&page=1"))
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content.length()").value(1));
    mvc.perform(get("/api/tickets?size=0")).andExpect(status().isBadRequest());
  }

  @Test
  void reportHalfOpenInterval() throws Exception {
    create();
    mvc.perform(get("/api/reports/summary?from=2025-12-31T00:00:00Z&to=2026-01-01T00:00:00Z"))
        .andExpect(jsonPath("$.createdCount").value(0));
    mvc.perform(get("/api/reports/summary?from=2026-01-01T00:00:00Z&to=2026-01-02T00:00:00Z"))
        .andExpect(jsonPath("$.createdCount").value(1))
        .andExpect(jsonPath("$.overdueOpenCount").value(1));
    mvc.perform(get("/api/reports/summary?from=2026-01-01T00:00:00Z&to=2026-01-01T00:00:00Z"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @Tag("e2e")
  void lifecycle() throws Exception {
    String id = create();
    mvc.perform(
            put("/api/tickets/" + id)
                .contentType("application/json")
                .content(
                    mapper.writeValueAsString(Map.of("description", "Updated", "priority", "LOW"))))
        .andExpect(status().isOk());
    change(id, "IN_PROGRESS", 200);
    change(id, "RESOLVED", 200);
    mvc.perform(get("/api/tickets/" + id))
        .andExpect(jsonPath("$.closedAt").value("2026-01-01T00:00:00Z"));
    mvc.perform(get("/api/tickets/" + id + "/history")).andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  @Tag("e2e")
  void forbiddenOperationKeepsData() throws Exception {
    String id = create();
    change(id, "CANCELLED", 200);
    String before =
        mvc.perform(get("/api/tickets/" + id)).andReturn().getResponse().getContentAsString();
    change(id, "IN_PROGRESS", 409);
    mvc.perform(
            put("/api/tickets/" + id)
                .contentType("application/json")
                .content(
                    mapper.writeValueAsString(Map.of("description", "Changed", "priority", "LOW"))))
        .andExpect(status().isConflict());
    assertEquals(
        before,
        mvc.perform(get("/api/tickets/" + id)).andReturn().getResponse().getContentAsString());
    mvc.perform(get("/api/tickets/" + id + "/history")).andExpect(jsonPath("$.length()").value(2));
  }
}
