package com.avega.tgd;

import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.dto.request.BatchTransactionRequestDto;
import com.avega.tgd.dto.request.TaxRuleRequestDto;
import com.avega.tgd.dto.request.TransactionRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Additional Controller Endpoints Integration Tests")
class AdditionalControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/auth/me should return authenticated user details")
    void testAuthMeEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .with(httpBasic("auditor", "auditor123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username", is("auditor")))
                .andExpect(jsonPath("$.data.authenticated", is(true)))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_AUDITOR")));
    }

    @Test
    @DisplayName("POST /api/v1/transactions/upload (wrapper) should accept BatchTransactionRequestDto")
    void testWrapperUploadEndpoint() throws Exception {
        TransactionRequestDto tx = new TransactionRequestDto(
                "WRAPPER-TX-001",
                LocalDate.now(),
                "CUST-WRAPPER",
                new BigDecimal("500.00"),
                new BigDecimal("0.18"),
                new BigDecimal("90.00"),
                com.avega.tgd.domain.enums.TransactionType.SALE,
                null
        );

        BatchTransactionRequestDto wrapper = new BatchTransactionRequestDto(List.of(tx));

        mockMvc.perform(post("/api/v1/transactions/upload")
                        .with(httpBasic("auditor", "auditor123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrapper)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalSuccess", is(1)));
    }

    @Test
    @DisplayName("POST /api/v1/transactions (single) should process transaction")
    void testSingleTransactionUpload() throws Exception {
        TransactionRequestDto tx = new TransactionRequestDto(
                "SINGLE-TX-001",
                LocalDate.now(),
                "CUST-SINGLE",
                new BigDecimal("1000.00"),
                new BigDecimal("0.18"),
                new BigDecimal("180.00"),
                com.avega.tgd.domain.enums.TransactionType.SALE,
                null
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(httpBasic("auditor", "auditor123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tx)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transactionId", is("SINGLE-TX-001")))
                .andExpect(jsonPath("$.data.complianceStatus", is("COMPLIANT")));

        // Query by transactionId
        mockMvc.perform(get("/api/v1/transactions/SINGLE-TX-001")
                        .with(httpBasic("auditor", "auditor123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionId", is("SINGLE-TX-001")));
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs should return paged audit trail")
    void testAuditLogsEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .with(httpBasic("auditor", "auditor123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", notNullValue()));

        mockMvc.perform(get("/api/v1/audit-logs/transaction/SINGLE-TX-001")
                        .with(httpBasic("auditor", "auditor123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("Admin can create, update, and toggle custom tax rules")
    void testAdminRuleCrud() throws Exception {
        TaxRuleRequestDto newRule = new TaxRuleRequestDto(
                "CUSTOM_TEST_RULE",
                "Custom test rule for testing",
                true,
                Severity.LOW,
                "{\"maxItems\": 10}"
        );

        // 1. Create rule as ADMIN
        String createResponse = mockMvc.perform(post("/api/v1/rules")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRule)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ruleName", is("CUSTOM_TEST_RULE")))
                .andReturn().getResponse().getContentAsString();

        Long ruleId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        // 2. Auditor attempting to toggle rule should be Forbidden (403)
        mockMvc.perform(put("/api/v1/rules/" + ruleId + "/toggle")
                        .with(httpBasic("auditor", "auditor123")))
                .andExpect(status().isForbidden());

        // 3. Admin toggles rule successfully
        mockMvc.perform(put("/api/v1/rules/" + ruleId + "/toggle")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled", is(false)));

        // 4. Admin updates rule
        newRule.setDescription("Updated description");
        mockMvc.perform(put("/api/v1/rules/" + ruleId)
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description", is("Updated description")));
    }
}
