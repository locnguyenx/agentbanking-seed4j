package com.agentbanking.rules.infrastructure.primary;

import com.agentbanking.rules.domain.port.in.ValidateTransactionUseCase;
import com.agentbanking.rules.domain.port.in.ValidateTransactionUseCase.ValidationResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rules")
@Validated
class RulesController {

  private final ValidateTransactionUseCase validateTransactionUseCase;

  public RulesController(ValidateTransactionUseCase validateTransactionUseCase) {
    this.validateTransactionUseCase = validateTransactionUseCase;
  }

  @PostMapping("/validate")
  public ResponseEntity<ValidationResult> validateTransaction(
      @Validated @RequestBody ValidateTransactionUseCase.TransactionValidationRequest request) {
    ValidationResult result = validateTransactionUseCase.validate(request);
    return ResponseEntity.ok(result);
  }
}