package com.agentbanking.settlement.domain.port.in;

import com.agentbanking.settlement.domain.model.SettlementResult;
import com.agentbanking.settlement.domain.model.SettlementType;
import java.nio.file.Path;
import java.time.LocalDate;

public interface ProcessSettlementUseCase {

  SettlementResult processDailySettlement(LocalDate date);
  
  SettlementResult generateSettlementReport(SettlementType type, LocalDate startDate, LocalDate endDate);
  
  Path generateSettlementFile(LocalDate date);
}