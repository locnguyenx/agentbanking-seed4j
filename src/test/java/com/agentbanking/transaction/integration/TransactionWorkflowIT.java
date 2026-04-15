package com.agentbanking.transaction.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agentbanking.IntegrationTest;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class TransactionWorkflowIT {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldProcessCashWithdrawal() throws Exception {
    String requestBody = """
        {
          "type": "CASH_WITHDRAWAL",
          "agentId": "AGT001",
          "amount": "500.00",
          "customerAccountId": "4111111111111111",
          "idempotencyKey": "withdrawal-test-001"
        }
        """;

    mockMvc
        .perform(
            post("/api/transactions/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").exists());
  }

  @Test
  void shouldProcessCashDeposit() throws Exception {
    String requestBody = """
        {
          "type": "CASH_DEPOSIT",
          "agentId": "AGT001",
          "amount": "1000.00",
          "customerAccountId": "4111111111111111",
          "idempotencyKey": "deposit-test-001"
        }
        """;

    mockMvc
        .perform(
            post("/api/transactions/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").exists());
  }

  @Test
  void shouldRejectInvalidTransactionType() throws Exception {
    String requestBody = """
        {
          "type": "INVALID_TYPE",
          "agentId": "AGT001",
          "amount": "500.00",
          "customerAccountId": "4111111111111111",
          "idempotencyKey": "test-001"
        }
        """;

    mockMvc
        .perform(
            post("/api/transactions/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }
}