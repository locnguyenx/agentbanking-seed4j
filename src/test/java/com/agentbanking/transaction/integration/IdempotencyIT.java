package com.agentbanking.transaction.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agentbanking.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class IdempotencyIT {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldReturnCachedResponseForDuplicateRequest() throws Exception {
    String requestBody = """
        {
          "type": "CASH_WITHDRAWAL",
          "agentId": "AGT001",
          "amount": "500.00",
          "customerAccountId": "4111111111111111",
          "idempotencyKey": "idempotency-test-001"
        }
        """;

    mockMvc
        .perform(
            post("/api/transactions/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").exists());

    mockMvc
        .perform(
            post("/api/transactions/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.transactionId").exists());
  }
}