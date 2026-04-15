package com.agentbanking.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agentbanking.redis")
public class RedisProperties {

  private String host = "localhost";
  private int port = 6379;
  private int database = 0;
  private String password;
  private int timeout = 5000;
  private int idleConnectionTimeout = 10000;

  public String getHost() { return host; }
  public void setHost(String host) { this.host = host; }
  public int getPort() { return port; }
  public void setPort(int port) { this.port = port; }
  public int getDatabase() { return database; }
  public void setDatabase(int database) { this.database = database; }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public int getTimeout() { return timeout; }
  public void setTimeout(int timeout) { this.timeout = timeout; }
  public int getIdleConnectionTimeout() { return idleConnectionTimeout; }
  public void setIdleConnectionTimeout(int idleConnectionTimeout) { this.idleConnectionTimeout = idleConnectionTimeout; }
}