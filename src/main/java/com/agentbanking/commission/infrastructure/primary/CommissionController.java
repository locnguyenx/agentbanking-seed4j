package com.agentbanking.commission.infrastructure.primary;

import com.agentbanking.commission.application.dto.CommissionEntryDTO;
import com.agentbanking.commission.domain.port.in.CalculateCommissionUseCase;
import com.agentbanking.commission.domain.port.in.SettleCommissionUseCase;
import com.agentbanking.shared.money.domain.Money;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/commission")
class CommissionController {

  private final CalculateCommissionUseCase calculateCommissionUseCase;
  private final SettleCommissionUseCase settleCommissionUseCase;

  public CommissionController(CalculateCommissionUseCase calculateCommissionUseCase,
                               SettleCommissionUseCase settleCommissionUseCase) {
    this.calculateCommissionUseCase = calculateCommissionUseCase;
    this.settleCommissionUseCase = settleCommissionUseCase;
  }

  @PostMapping("/calculate")
  public ResponseEntity<CommissionEntryDTO> calculate(
      @RequestBody Map<String, Object> request) {
    var entry = calculateCommissionUseCase.calculate(
      (String) request.get("transactionId"),
      (String) request.get("agentId"),
      (String) request.get("type"),
      Money.of(new BigDecimal(request.get("amount").toString()))
    );
    return ResponseEntity.ok(CommissionEntryDTO.fromDomain(entry));
  }

  @PostMapping("/settle")
  public ResponseEntity<Integer> settle() {
    return ResponseEntity.ok(settleCommissionUseCase.settlePendingCommissions());
  }
}
