package com.agentbanking.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentbanking.temporal")
public class TemporalProperties {

  private String namespace = "agentbanking.default";
  private String host = "localhost";
  private int port = 7233;
  private String taskQueue = "TRANSACTION_TASK_QUEUE";
  private String connectionTimeout = "30s";
  private String executionTimeout = "5m";

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getTaskQueue() {
    return taskQueue;
  }

  public void setTaskQueue(String taskQueue) {
    this.taskQueue = taskQueue;
  }

  public String getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(String connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public String getExecutionTimeout() {
    return executionTimeout;
  }

  public void setExecutionTimeout(String executionTimeout) {
    this.executionTimeout = executionTimeout;
  }
}
