package com.agentbanking.gateway.wire.gateway.infrastructure.primary;

import static org.assertj.core.api.Assertions.assertThat;

import com.agentbanking.gateway.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@IntegrationTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayResourceIT {

  @Autowired
  private WebTestClient webTestClient;

  @LocalServerPort
  private int port;

  @Test
  void shouldReturnGatewayRoutes() {
    webTestClient
      .get()
      .uri("/api/gateway/routes")
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .jsonPath("$").isArray();
  }

  @Test
  void shouldExposeActuatorHealth() {
    webTestClient
      .get()
      .uri("/actuator/health")
      .exchange()
      .expectStatus()
      .isOk()
      .expectBody()
      .jsonPath("$.status").isEqualTo("UP");
  }
}
