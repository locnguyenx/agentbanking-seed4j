package com.agentbanking.ledger.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.agentbanking.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class LedgerWorkflowIT {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldRetrieveLedgerEntriesForTransaction() throws Exception {
    mockMvc
        .perform(get("/api/ledger/transaction/TXN001/entries"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void shouldReturnEmptyListForUnknownTransaction() throws Exception {
    mockMvc
        .perform(get("/api/ledger/transaction/UNKNOWN/entries"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }
}