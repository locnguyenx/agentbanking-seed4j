package com.agentbanking.settlement.infrastructure.primary;

import com.agentbanking.settlement.domain.model.SettlementResult;
import com.agentbanking.settlement.domain.model.SettlementType;
import com.agentbanking.settlement.domain.port.in.ProcessSettlementUseCase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settlement")
class SettlementController {

  private final ProcessSettlementUseCase settlementUseCase;

  public SettlementController(ProcessSettlementUseCase settlementUseCase) {
    this.settlementUseCase = settlementUseCase;
  }

  @PostMapping("/daily")
  public ResponseEntity<SettlementResult> processDaily(
      @RequestBody Map<String, String> request) {
    LocalDate date = LocalDate.parse(request.get("date"));
    return ResponseEntity.ok(settlementUseCase.processDailySettlement(date));
  }

  @PostMapping("/report")
  public ResponseEntity<SettlementResult> generateReport(
      @RequestBody Map<String, String> request) {
    SettlementType type = SettlementType.valueOf(request.get("type"));
    LocalDate startDate = LocalDate.parse(request.get("startDate"));
    LocalDate endDate = LocalDate.parse(request.get("endDate"));
    return ResponseEntity.ok(settlementUseCase.generateSettlementReport(type, startDate, endDate));
  }

  @GetMapping("/file/{date}")
  public ResponseEntity<byte[]> downloadSettlementFile(
      @PathVariable String date) throws IOException {
    LocalDate localDate = LocalDate.parse(date);
    Path csvPath = settlementUseCase.generateSettlementFile(localDate);
    
    byte[] fileContent = Files.readAllBytes(csvPath);
    String filename = csvPath.getFileName().toString();
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(fileContent);
  }
}