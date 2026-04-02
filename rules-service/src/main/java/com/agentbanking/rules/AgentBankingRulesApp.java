package com.agentbanking.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import com.agentbanking.rules.shared.generation.domain.ExcludeFromGeneratedCodeCoverage;

@SpringBootApplication
@ExcludeFromGeneratedCodeCoverage(reason = "Not testing logs")
public class AgentBankingRulesApp {

  private static final Logger log = LoggerFactory.getLogger(AgentBankingRulesApp.class);

  public static void main(String[] args) {
    Environment env = SpringApplication.run(AgentBankingRulesApp.class, args).getEnvironment();

    if (log.isInfoEnabled()) {
      log.info(ApplicationStartupTraces.of(env));
    }
  }
}
