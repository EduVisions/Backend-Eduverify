package com.eduverify.platform.academicmanagement.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndLogin(String role) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@universidad.edu";

        mockMvc.perform(post("/api/iam/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Test1234!", "role", role
                        ))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/iam/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Test1234!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> body = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
        return (String) body.get("token");
    }

    @Test
    void listExamsWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/exams"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void teacherCreatesExamAndStudentCompletesFullFlow() throws Exception {
        String teacherToken = registerAndLogin("teacher");
        String studentToken = registerAndLogin("student");

        MvcResult createResult = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Calculo Diferencial - Parcial 2",
                                "scheduledDate", LocalDateTime.now().plusDays(1).toString(),
                                "durationMinutes", 90
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn();

        Map<?, ?> exam = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        String examId = (String) exam.get("id");

        mockMvc.perform(post("/api/exams/" + examId + "/start")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        MvcResult sessionResult = mockMvc.perform(post("/api/exams/" + examId + "/sessions")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn();

        Map<?, ?> session = objectMapper.readValue(sessionResult.getResponse().getContentAsString(), Map.class);
        String sessionId = (String) session.get("id");

        mockMvc.perform(put("/api/exams/" + examId + "/sessions/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCount").value(1));

        mockMvc.perform(get("/api/exams/" + examId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void gettingUnknownExamReturnsNotFound() throws Exception {
        String token = registerAndLogin("teacher");

        mockMvc.perform(get("/api/exams/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
