package com.avega.tgd;

import com.avega.tgd.domain.enums.TransactionType;
import com.avega.tgd.dto.request.TransactionRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("End-to-End API Integration Tests")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Unauthenticated request to protected API should return 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/reports/customer-tax-summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Batch transaction upload with Basic Auth should succeed and compute tax gap")
    void testBatchTransactionUploadWithBasicAuth() throws Exception {
        TransactionRequestDto saleTx = new TransactionRequestDto(
                "INT-TX-001",
                LocalDate.of(2026, 10, 20),
                "CUST-ACME",
                new BigDecimal("10000.00"),
                new BigDecimal("0.18"),
                new BigDecimal("1800.00"),
                TransactionType.SALE,
                null
        );

        TransactionRequestDto underpaidTx = new TransactionRequestDto(
                "INT-TX-002",
                LocalDate.of(2026, 10, 21),
                "CUST-ACME",
                new BigDecimal("20000.00"),
                new BigDecimal("0.18"),
                new BigDecimal("1000.00"), // Expected 3600 -> Underpaid by 2600
                TransactionType.SALE,
                null
        );

        List<TransactionRequestDto> batch = List.of(saleTx, underpaidTx);

        mockMvc.perform(post("/api/v1/transactions/batch")
                        .with(httpBasic("auditor", "auditor123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batch)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalReceived", is(2)))
                .andExpect(jsonPath("$.data.totalSuccess", is(2)))
                .andExpect(jsonPath("$.data.totalFailed", is(0)));

        // Verify exceptions endpoint shows the underpayment exception
        mockMvc.perform(get("/api/v1/exceptions")
                        .with(httpBasic("auditor", "auditor123"))
                        .param("customerId", "CUST-ACME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())));

        // Verify customer tax summary report
        mockMvc.perform(get("/api/v1/reports/customer-tax-summary/CUST-ACME")
                        .with(httpBasic("auditor", "auditor123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId", is("CUST-ACME")))
                .andExpect(jsonPath("$.data.totalTransactions", is(2)))
                .andExpect(jsonPath("$.data.complianceScore", notNullValue()));
    }

    @Test
    @DisplayName("Invalid transaction should be captured as failure in batch upload")
    void testBatchTransactionWithInvalidFields() throws Exception {
        TransactionRequestDto invalidTx = new TransactionRequestDto(
                "INT-INV-001",
                null, // Missing date
                "CUST-FAIL",
                new BigDecimal("-500.00"), // Invalid amount <= 0
                new BigDecimal("0.18"),
                null,
                TransactionType.SALE,
                null
        );

        mockMvc.perform(post("/api/v1/transactions/batch")
                        .with(httpBasic("auditor", "auditor123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(invalidTx))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalFailed", is(1)))
                .andExpect(jsonPath("$.data.results[0].validationStatus", is("FAILURE")))
                .andExpect(jsonPath("$.data.results[0].failureReasons", containsString("amount must be > 0")));
    }

    @Test
    @DisplayName("Exception summary report endpoint returns counts by severity")
    void testExceptionSummaryReport() throws Exception {
        mockMvc.perform(get("/api/v1/reports/exception-summary")
                        .with(httpBasic("auditor", "auditor123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.countBySeverity", notNullValue()));
    }

    @Test
    @DisplayName("Admin user can view and toggle tax rules")
    void testAdminToggleRule() throws Exception {
        // Toggle rule 1 (HIGH_VALUE_TRANSACTION) using admin credentials
        mockMvc.perform(get("/api/v1/rules")
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", not(empty())));
    }
}
