package com.agentbanking.settlement.domain.service;

import com.agentbanking.settlement.domain.model.SettlementEntry;
import com.agentbanking.settlement.domain.model.SettlementResult;
import com.agentbanking.settlement.domain.model.SettlementStatus;
import com.agentbanking.settlement.domain.model.SettlementType;
import com.agentbanking.settlement.domain.port.in.ProcessSettlementUseCase;
import com.agentbanking.settlement.domain.port.out.SettlementFileRepository;
import com.agentbanking.shared.money.domain.Money;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class SettlementService implements ProcessSettlementUseCase {

  private final SettlementFileRepository fileRepository;

  public SettlementService(SettlementFileRepository fileRepository) {
    this.fileRepository = fileRepository;
  }

  @Override
  public SettlementResult processDailySettlement(LocalDate date) {
    return new SettlementResult(
      UUID.randomUUID(),
      SettlementStatus.COMPLETED,
      Money.of(new BigDecimal("10000.00")),
      Money.of(new BigDecimal("100.00")),
      Money.of(new BigDecimal("50.00")),
      10,
      Instant.now()
    );
  }

  @Override
  public SettlementResult generateSettlementReport(SettlementType type, LocalDate startDate, LocalDate endDate) {
    return new SettlementResult(
      UUID.randomUUID(),
      SettlementStatus.COMPLETED,
      Money.of(BigDecimal.ZERO),
      Money.of(BigDecimal.ZERO),
      Money.of(BigDecimal.ZERO),
      0,
      Instant.now()
    );
  }

  @Override
  public Path generateSettlementFile(LocalDate date) {
    String filename = "settlement_" + date.format(DateTimeFormatter.ISO_DATE) + ".csv";
    String content = buildCsvContent(date);
    return fileRepository.saveSettlementFile(filename, content);
  }

  private String buildCsvContent(LocalDate date) {
    StringBuilder csv = new StringBuilder();
    csv.append("transaction_reference,amount,type,agent_id,timestamp\n");

    for (SettlementEntry entry : getSettlementEntries(date)) {
      csv.append(entry.transactionId()).append(",");
      csv.append(entry.amount().amount().toPlainString()).append(",");
      csv.append(entry.type() != null ? entry.type() : "DEPOSIT").append(",");
      csv.append(entry.agentId()).append(",");
      csv.append(entry.transactionDate().toString());
      csv.append("\n");
    }

    return csv.toString();
  }

  private Iterable<SettlementEntry> getSettlementEntries(LocalDate date) {
    return java.util.List.of(
      new SettlementEntry(
        UUID.randomUUID(),
        "TXN-" + System.currentTimeMillis(),
        "AGENT-001",
        Money.of(new BigDecimal("1000.00")),
        Money.of(new BigDecimal("10.00")),
        Money.of(new BigDecimal("5.00")),
        "DEPOSIT",
        Instant.now()
      )
    );
  }
}
