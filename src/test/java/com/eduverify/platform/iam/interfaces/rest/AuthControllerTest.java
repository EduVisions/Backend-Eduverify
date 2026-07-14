package com.eduverify.platform.iam.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@universidad.edu";
    }

    @Test
    void registerReturnsCreatedUser() throws Exception {
        String email = uniqueEmail();

        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Test1234!",
                                "role", "student"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void registerRejectsUnknownRole() throws Exception {
        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", uniqueEmail(),
                                "password", "Test1234!",
                                "role", "admin"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        Map<String, String> body = Map.of("email", email, "password", "Test1234!", "role", "teacher");

        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsTokenForCorrectCredentials() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Test1234!", "role", "student"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/iam/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Test1234!", "role", "student"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/iam/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized());
    }
}
