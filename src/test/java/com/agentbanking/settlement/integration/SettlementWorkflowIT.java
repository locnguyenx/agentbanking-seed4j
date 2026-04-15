package com.agentbanking.settlement.integration;

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
class SettlementWorkflowIT {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldProcessDailySettlement() throws Exception {
    String requestBody = """
        {
          "date": "2024-01-15"
        }
        """;

    mockMvc
        .perform(
            post("/api/settlement/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settlementId").exists());
  }

  @Test
  void shouldGenerateSettlementReport() throws Exception {
    String requestBody = """
        {
          "type": "DAILY",
          "startDate": "2024-01-01",
          "endDate": "2024-01-31"
        }
        """;

    mockMvc
        .perform(
            post("/api/settlement/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.settlementId").exists());
  }

  @Test
  void shouldDetectDiscrepanciesInSettlement() throws Exception {
    String requestBody = """
        {
          "type": "DISCREPANCY",
          "startDate": "2024-01-01",
          "endDate": "2024-01-31"
        }
        """;

    mockMvc
        .perform(
            post("/api/settlement/report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk());
  }
}