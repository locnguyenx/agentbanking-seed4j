package com.agentbanking.floatagg.infrastructure.primary;

import com.agentbanking.floatagg.domain.port.in.BalanceInquiryUseCase;
import com.agentbanking.floatagg.domain.port.in.BalanceInquiryUseCase.BalanceResult;
import com.agentbanking.floatagg.domain.port.in.CreditFloatUseCase;
import com.agentbanking.floatagg.domain.port.in.CreditFloatUseCase.CreditResult;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/float")
class FloatController {

  private final BalanceInquiryUseCase balanceInquiryUseCase;
  private final CreditFloatUseCase creditFloatUseCase;

  public FloatController(BalanceInquiryUseCase balanceInquiryUseCase,
                         CreditFloatUseCase creditFloatUseCase) {
    this.balanceInquiryUseCase = balanceInquiryUseCase;
    this.creditFloatUseCase = creditFloatUseCase;
  }

  @GetMapping("/{agentId}/balance")
  public ResponseEntity<BalanceResult> getBalance(@PathVariable String agentId) {
    var agentIdObj = AgentId.of(agentId);
    return ResponseEntity.ok(balanceInquiryUseCase.getBalance(agentIdObj));
  }

  @PostMapping("/{agentId}/credit")
  public ResponseEntity<CreditResult> credit(
      @PathVariable String agentId,
      @RequestBody Map<String, BigDecimal> request) {
    var agentIdObj = AgentId.of(agentId);
    var amount = Money.of(request.get("amount"));
    return ResponseEntity.ok(creditFloatUseCase.credit(agentIdObj, amount));
  }
}