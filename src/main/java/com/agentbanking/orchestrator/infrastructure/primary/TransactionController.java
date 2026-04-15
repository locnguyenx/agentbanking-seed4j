package com.agentbanking.orchestrator.infrastructure.primary;

import com.agentbanking.orchestrator.application.dto.TransactionRequestDTO;
import com.agentbanking.orchestrator.application.dto.TransactionResponseDTO;
import com.agentbanking.orchestrator.domain.service.TransactionProcessingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/transaction")
@Validated
class TransactionController {

  private final TransactionProcessingService transactionProcessingService;

  public TransactionController(TransactionProcessingService transactionProcessingService) {
    this.transactionProcessingService = transactionProcessingService;
  }

   @PostMapping("/execute")
   public ResponseEntity<TransactionResponseDTO> executeTransaction(
       @Validated @RequestBody TransactionRequestDTO request) {
    var response = transactionProcessingService.processTransaction(request.toDomain());
    return ResponseEntity.ok(TransactionResponseDTO.fromDomain(response));
  }

  @GetMapping("/{sagaId}/status")
  public ResponseEntity<TransactionResponseDTO> getStatus(@PathVariable String sagaId) {
    var response = transactionProcessingService.getTransactionStatus(sagaId);
    return ResponseEntity.ok(TransactionResponseDTO.fromDomain(response));
  }

  @PostMapping("/{sagaId}/cancel")
  public ResponseEntity<Map<String, Boolean>> cancel(
      @PathVariable String sagaId, 
      @RequestBody Map<String, String> request) {
    boolean result = transactionProcessingService.cancelTransaction(sagaId);
    return ResponseEntity.ok(Map.of("cancelled", result));
  }
}
