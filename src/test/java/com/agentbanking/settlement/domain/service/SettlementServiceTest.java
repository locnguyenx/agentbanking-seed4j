package com.agentbanking.settlement.domain.service;

import static org.assertj.core.api.Assertions.*;

import com.agentbanking.UnitTest;
import com.agentbanking.settlement.domain.model.SettlementResult;
import com.agentbanking.settlement.domain.model.SettlementStatus;
import com.agentbanking.settlement.domain.model.SettlementType;
import com.agentbanking.settlement.domain.port.out.SettlementFileRepository;
import com.agentbanking.shared.money.domain.Money;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@UnitTest
@DisplayName("SettlementService")
class SettlementServiceTest {

  private SettlementService settlementService;
  private SettlementFileRepository fileRepository;

  @TempDir
  Path tempDir;

  @BeforeEach
  void setUp() {
    fileRepository = new TestSettlementFileRepository(tempDir);
    settlementService = new SettlementService(fileRepository);
  }

  @Nested
  class ProcessDailySettlement {

    @Test
    void shouldProcessDailySettlementSuccessfully() {
      LocalDate date = LocalDate.of(2026, 4, 7);

      SettlementResult result = settlementService.processDailySettlement(date);

      assertThat(result.settlementId()).isNotNull();
      assertThat(result.status()).isEqualTo(SettlementStatus.COMPLETED);
      assertThat(result.totalAmount()).isEqualTo(Money.of(new BigDecimal("10000.00")));
      assertThat(result.totalFee()).isEqualTo(Money.of(new BigDecimal("100.00")));
      assertThat(result.totalCommission()).isEqualTo(Money.of(new BigDecimal("50.00")));
      assertThat(result.transactionCount()).isEqualTo(10);
      assertThat(result.processedAt()).isNotNull();
    }

    @Test
    void shouldProcessDailySettlementForDifferentDates() {
      LocalDate date1 = LocalDate.of(2026, 4, 6);
      LocalDate date2 = LocalDate.of(2026, 4, 7);

      SettlementResult result1 = settlementService.processDailySettlement(date1);
      SettlementResult result2 = settlementService.processDailySettlement(date2);

      assertThat(result1.settlementId()).isNotEqualTo(result2.settlementId());
    }

    @Test
    void shouldGenerateUniqueSettlementIdForEachCall() {
      SettlementResult result1 = settlementService.processDailySettlement(LocalDate.now());
      SettlementResult result2 = settlementService.processDailySettlement(LocalDate.now());

      assertThat(result1.settlementId()).isNotEqualTo(result2.settlementId());
    }

    @Test
    void shouldReturnCompletedStatus() {
      SettlementResult result = settlementService.processDailySettlement(LocalDate.now());

      assertThat(result.status()).isEqualTo(SettlementStatus.COMPLETED);
    }

    @Test
    void shouldReturnPositiveTransactionCount() {
      SettlementResult result = settlementService.processDailySettlement(LocalDate.now());

      assertThat(result.transactionCount()).isPositive();
    }

    @Test
    void shouldReturnPositiveTotalAmount() {
      SettlementResult result = settlementService.processDailySettlement(LocalDate.now());

      assertThat(result.totalAmount().isPositive()).isTrue();
    }
  }

  @Nested
  class GenerateSettlementReport {

    @Test
    void shouldGenerateDailyReport() {
      LocalDate start = LocalDate.of(2026, 4, 1);
      LocalDate end = LocalDate.of(2026, 4, 7);

      SettlementResult result = settlementService.generateSettlementReport(SettlementType.DAILY, start, end);

      assertThat(result.settlementId()).isNotNull();
      assertThat(result.status()).isEqualTo(SettlementStatus.COMPLETED);
      assertThat(result.totalAmount()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.totalFee()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.totalCommission()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.transactionCount()).isZero();
      assertThat(result.processedAt()).isNotNull();
    }

    @Test
    void shouldGenerateWeeklyReport() {
      LocalDate start = LocalDate.of(2026, 3, 30);
      LocalDate end = LocalDate.of(2026, 4, 5);

      SettlementResult result = settlementService.generateSettlementReport(SettlementType.WEEKLY, start, end);

      assertThat(result.status()).isEqualTo(SettlementStatus.COMPLETED);
      assertThat(result.settlementId()).isNotNull();
    }

    @Test
    void shouldGenerateMonthlyReport() {
      LocalDate start = LocalDate.of(2026, 3, 1);
      LocalDate end = LocalDate.of(2026, 3, 31);

      SettlementResult result = settlementService.generateSettlementReport(SettlementType.MONTHLY, start, end);

      assertThat(result.status()).isEqualTo(SettlementStatus.COMPLETED);
      assertThat(result.settlementId()).isNotNull();
    }

    @Test
    void shouldGenerateUniqueReportIdForEachCall() {
      LocalDate start = LocalDate.of(2026, 4, 1);
      LocalDate end = LocalDate.of(2026, 4, 7);

      SettlementResult result1 = settlementService.generateSettlementReport(SettlementType.DAILY, start, end);
      SettlementResult result2 = settlementService.generateSettlementReport(SettlementType.DAILY, start, end);

      assertThat(result1.settlementId()).isNotEqualTo(result2.settlementId());
    }

    @Test
    void shouldReturnZeroValuesForEmptyReport() {
      LocalDate start = LocalDate.of(2026, 1, 1);
      LocalDate end = LocalDate.of(2026, 1, 31);

      SettlementResult result = settlementService.generateSettlementReport(SettlementType.MONTHLY, start, end);

      assertThat(result.totalAmount().isZero()).isTrue();
      assertThat(result.totalFee().isZero()).isTrue();
      assertThat(result.totalCommission().isZero()).isTrue();
      assertThat(result.transactionCount()).isZero();
    }

    @Test
    void shouldGenerateReportForSameDayRange() {
      LocalDate singleDay = LocalDate.of(2026, 4, 7);

      SettlementResult result = settlementService.generateSettlementReport(SettlementType.DAILY, singleDay, singleDay);

      assertThat(result.status()).isEqualTo(SettlementStatus.COMPLETED);
    }

    @Test
    void shouldGenerateReportForAllSettlementTypes() {
      LocalDate start = LocalDate.of(2026, 4, 1);
      LocalDate end = LocalDate.of(2026, 4, 30);

      for (SettlementType type : SettlementType.values()) {
        SettlementResult result = settlementService.generateSettlementReport(type, start, end);

        assertThat(result.settlementId()).isNotNull();
        assertThat(result.status()).isEqualTo(SettlementStatus.COMPLETED);
        assertThat(result.processedAt()).isNotNull();
      }
    }
  }

  @Nested
  class GenerateSettlementFile {

    @Test
    void shouldGenerateCsvFileForDailySettlement() {
      LocalDate date = LocalDate.of(2026, 4, 7);

      Path csvPath = settlementService.generateSettlementFile(date);

      assertThat(csvPath).exists();
      assertThat(csvPath.toString()).endsWith(".csv");
    }

    @Test
    void shouldGenerateCsvFileWithCorrectFormat() throws IOException {
      LocalDate date = LocalDate.of(2026, 4, 7);

      Path csvPath = settlementService.generateSettlementFile(date);

      String content = Files.readString(csvPath);
      String[] lines = content.split("\n");
      
      assertThat(lines).isNotEmpty();
      assertThat(lines[0]).contains("transaction_reference");
      assertThat(lines[0]).contains("amount");
      assertThat(lines[0]).contains("type");
      assertThat(lines[0]).contains("agent_id");
      assertThat(lines[0]).contains("timestamp");
    }

    @Test
    void shouldGenerateCsvFileWithTransactionData() throws IOException {
      LocalDate date = LocalDate.of(2026, 4, 7);

      Path csvPath = settlementService.generateSettlementFile(date);

      String content = Files.readString(csvPath);
      String[] lines = content.split("\n");
      
      assertThat(lines.length).isGreaterThan(1);
    }

    @Test
    void shouldGenerateCsvFileWithCorrectDateInFilename() {
      LocalDate date = LocalDate.of(2026, 4, 7);

      Path csvPath = settlementService.generateSettlementFile(date);

      String filename = csvPath.getFileName().toString();
      assertThat(filename).contains("2026-04-07");
    }

    @Test
    void shouldGenerateSeparateFilesForDifferentDates() {
      LocalDate date1 = LocalDate.of(2026, 4, 6);
      LocalDate date2 = LocalDate.of(2026, 4, 7);

      Path csvPath1 = settlementService.generateSettlementFile(date1);
      Path csvPath2 = settlementService.generateSettlementFile(date2);

      assertThat(csvPath1).exists();
      assertThat(csvPath2).exists();
      assertThat(csvPath1).isNotEqualTo(csvPath2);
    }

    @Test
    void shouldGenerateCsvFileInSettlementDirectory() {
      LocalDate date = LocalDate.of(2026, 4, 7);

      Path csvPath = settlementService.generateSettlementFile(date);

      assertThat(csvPath.getParent().toString()).contains("settlement-files");
    }
  }

  static class TestSettlementFileRepository implements SettlementFileRepository {
    private final Path baseDir;

    TestSettlementFileRepository(Path baseDir) {
      this.baseDir = baseDir;
    }

    @Override
    public Path saveSettlementFile(String filename, String content) {
      try {
        Path dir = baseDir.resolve("settlement-files");
        java.nio.file.Files.createDirectories(dir);
        Path file = dir.resolve(filename);
        java.nio.file.Files.writeString(file, content);
        return file;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
