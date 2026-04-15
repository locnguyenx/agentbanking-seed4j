package com.agentbanking.orchestrator.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

  @Bean
  public WorkflowServiceStubs workflowServiceStubs(TemporalProperties props) {
    WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
            .setTarget(props.getHost() + ":" + props.getPort())
            .build();
    return WorkflowServiceStubs.newServiceStubs(options);
  }

  @Bean
  public WorkflowClient workflowClient(WorkflowServiceStubs stubs, TemporalProperties props) {
    return WorkflowClient.newInstance(stubs,
            WorkflowClientOptions.newBuilder()
                    .setNamespace(props.getNamespace())
                    .build());
  }
}
