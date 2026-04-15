package com.agentbanking.rules.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agentbanking.IntegrationTest;
import com.agentbanking.shared.transaction.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class RulesWorkflowIT {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldCalculateFeeForTransaction() throws Exception {
    String requestBody = """
        {
          "type": "CASH_WITHDRAWAL",
          "agentId": "AGT001",
          "amount": "500.00",
          "customerAccountId": "4111111111111111"
        }
        """;

    mockMvc
        .perform(
            post("/api/rules/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approved").exists())
        .andExpect(jsonPath("$.feeAmount").exists());
  }

  @Test
  void shouldRejectTransactionExceedingVelocityLimit() throws Exception {
    String requestBody = """
        {
          "type": "CASH_WITHDRAWAL",
          "agentId": "AGT001",
          "amount": "50000.00",
          "customerAccountId": "4111111111111111"
        }
        """;

    mockMvc
        .perform(
            post("/api/rules/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approved").value(false));
  }

  @Test
  void shouldRejectTransactionBelowMinimumAmount() throws Exception {
    String requestBody = """
        {
          "type": "CASH_WITHDRAWAL",
          "agentId": "AGT001",
          "amount": "1.00",
          "customerAccountId": "4111111111111111"
        }
        """;

    mockMvc
        .perform(
            post("/api/rules/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approved").value(false));
  }
}