package com.agentbanking.onboarding.integration;

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
class OnboardingWorkflowIT {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldRegisterNewAgent() throws Exception {
    String requestBody = """
        {
          "agentName": "John Doe",
          "icNumber": "123456789012",
          "phoneNumber": "+60123456789",
          "email": "john.doe@example.com",
          "address": "123 Street, City",
          "outletName": "Test Outlet",
          "outletCode": "OUT001"
        }
        """;

    mockMvc
        .perform(
            post("/api/onboarding/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.agentId").exists());
  }

  @Test
  void shouldRejectInvalidEmailFormat() throws Exception {
    String requestBody = """
        {
          "agentName": "John Doe",
          "icNumber": "123456789012",
          "phoneNumber": "+60123456789",
          "email": "invalid-email",
          "address": "123 Street, City",
          "outletName": "Test Outlet",
          "outletCode": "OUT001"
        }
        """;

    mockMvc
        .perform(
            post("/api/onboarding/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldSubmitKycVerification() throws Exception {
    String requestBody = """
        {
          "agentId": "AGT001",
          "icNumber": "123456789012",
          "selfieImage": "base64encodedimage",
          "icFrontImage": "base64encodedimage",
          "icBackImage": "base64encodedimage"
        }
        """;

    mockMvc
        .perform(
            post("/api/onboarding/kyc/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk());
  }
}