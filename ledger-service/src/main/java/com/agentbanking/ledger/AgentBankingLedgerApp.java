package com.agentbanking.ledger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import com.agentbanking.ledger.shared.generation.domain.ExcludeFromGeneratedCodeCoverage;

@SpringBootApplication
@ExcludeFromGeneratedCodeCoverage(reason = "Not testing logs")
public class AgentBankingLedgerApp {

  private static final Logger log = LoggerFactory.getLogger(AgentBankingLedgerApp.class);

  public static void main(String[] args) {
    Environment env = SpringApplication.run(AgentBankingLedgerApp.class, args).getEnvironment();

    if (log.isInfoEnabled()) {
      log.info(ApplicationStartupTraces.of(env));
    }
  }
}
