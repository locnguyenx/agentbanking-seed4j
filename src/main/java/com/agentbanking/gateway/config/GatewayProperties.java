package com.agentbanking.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentbanking.gateway")
public class GatewayProperties {

  private String defaultTimeout = "30s";
  private int maxRequestBodySize = 10240;
  private boolean retryEnabled = true;
  private int retryAttempts = 3;

  public String getDefaultTimeout() { return defaultTimeout; }
  public void setDefaultTimeout(String defaultTimeout) { this.defaultTimeout = defaultTimeout; }
  public int getMaxRequestBodySize() { return maxRequestBodySize; }
  public void setMaxRequestBodySize(int maxRequestBodySize) { this.maxRequestBodySize = maxRequestBodySize; }
  public boolean isRetryEnabled() { return retryEnabled; }
  public void setRetryEnabled(boolean retryEnabled) { this.retryEnabled = retryEnabled; }
  public int getRetryAttempts() { return retryAttempts; }
  public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
}