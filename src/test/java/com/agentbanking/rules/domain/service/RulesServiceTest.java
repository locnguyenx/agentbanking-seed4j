package com.agentbanking.rules.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agentbanking.UnitTest;
import com.agentbanking.shared.onboarding.domain.AgentType;
import com.agentbanking.rules.domain.model.FeeCalculationResult;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.rules.domain.model.VelocityRule;
import com.agentbanking.rules.domain.model.VelocityScope;
import com.agentbanking.rules.domain.port.in.ValidateTransactionUseCase.TransactionValidationRequest;
import com.agentbanking.rules.domain.port.in.ValidateTransactionUseCase.ValidationResult;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
@DisplayName("RulesService")
class RulesServiceTest {

  @Mock
  private FeeConfigRepository feeConfigRepository;

  @Mock
  private VelocityRuleRepository velocityRuleRepository;

  private FeeCalculationService feeCalculationService;

  private RulesService rulesService;

  @BeforeEach
  void setUp() {
    feeCalculationService = new FeeCalculationService();
    rulesService = new RulesService(feeConfigRepository, velocityRuleRepository, feeCalculationService);
  }

  private FeeConfig createActiveFeeConfig() {
    return new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.STANDARD,
      FeeType.FIXED,
      Money.of(new BigDecimal("1.50")),
      Money.of(new BigDecimal("0.50")),
      Money.of(new BigDecimal("1.00")),
      Money.of(new BigDecimal("10000.00")),
      50,
      LocalDate.now().minusDays(1),
      LocalDate.now().plusDays(30)
    );
  }

  private FeeConfig createPercentageFeeConfig() {
    return new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.STANDARD,
      FeeType.PERCENTAGE,
      Money.of(new BigDecimal("1.5")),
      Money.of(new BigDecimal("0.5")),
      Money.of(new BigDecimal("1.0")),
      Money.of(new BigDecimal("10000.00")),
      50,
      LocalDate.now().minusDays(1),
      LocalDate.now().plusDays(30)
    );
  }

  private TransactionValidationRequest createValidRequest() {
    return new TransactionValidationRequest(
      TransactionType.CASH_WITHDRAWAL,
      AgentId.of("AGT-001"),
      AgentType.STANDARD,
      Money.of(new BigDecimal("100.00")),
      "123456789012"
    );
  }

  @Nested
  class ValidateTransaction {

    @Test
    void shouldReturnValidWhenAllChecksPass() {
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(
        TransactionType.CASH_WITHDRAWAL, AgentType.STANDARD))
        .thenReturn(Optional.of(createActiveFeeConfig()));
      when(velocityRuleRepository.findActiveByScope(VelocityScope.PER_AGENT))
        .thenReturn(Optional.empty());

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isTrue();
      assertThat(result.errors()).isEmpty();
      assertThat(result.feeCalculation()).isNotNull();
      assertThat(result.feeCalculation().isValid()).isTrue();
    }

    @Test
    void shouldReturnValidWhenVelocityRulesExistButNotExceeded() {
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(createActiveFeeConfig()));
      VelocityRule nonExceededRule = new VelocityRule(
        UUID.randomUUID(),
        VelocityScope.PER_AGENT,
        100,
        Money.of(new BigDecimal("50000.00")),
        true
      );
      when(velocityRuleRepository.findActiveByScope(VelocityScope.PER_AGENT))
        .thenReturn(Optional.of(nonExceededRule));

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isTrue();
      assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldReturnInvalidWhenAmountIsZero() {
      TransactionValidationRequest request = new TransactionValidationRequest(
        TransactionType.CASH_WITHDRAWAL,
        AgentId.of("AGT-001"),
        AgentType.STANDARD,
        Money.of(BigDecimal.ZERO),
        "123456789012"
      );
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(createActiveFeeConfig()));
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.empty());

      ValidationResult result = rulesService.validate(request);

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("positive"));
    }

    @Test
    void shouldReturnInvalidWhenAmountIsNegative() {
      TransactionValidationRequest request = new TransactionValidationRequest(
        TransactionType.CASH_WITHDRAWAL,
        AgentId.of("AGT-001"),
        AgentType.STANDARD,
        Money.of(new BigDecimal("-50.00")),
        "123456789012"
      );
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(createActiveFeeConfig()));
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.empty());

      ValidationResult result = rulesService.validate(request);

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("positive"));
    }

    @Test
    void shouldReturnInvalidWhenFeeConfigNotFound() {
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.empty());
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.empty());

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("Fee configuration"));
      assertThat(result.feeCalculation()).isNull();
    }

    @Test
    void shouldReturnInvalidWhenFeeConfigExpired() {
      FeeConfig expiredConfig = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("1.50")),
        Money.of(new BigDecimal("0.50")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(60),
        LocalDate.now().minusDays(1)
      );
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(expiredConfig));
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.empty());

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("not active"));
      assertThat(result.feeCalculation()).isNull();
    }

    @Test
    void shouldReturnInvalidWhenFeeConfigNotYetEffective() {
      FeeConfig futureConfig = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("1.50")),
        Money.of(new BigDecimal("0.50")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(60)
      );
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(futureConfig));
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.empty());

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("not active"));
      assertThat(result.feeCalculation()).isNull();
    }

    @Test
    void shouldReturnInvalidWhenVelocityLimitExceededByCount() {
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(createActiveFeeConfig()));
      VelocityRule exceededRule = new VelocityRule(
        UUID.randomUUID(),
        VelocityScope.PER_AGENT,
        1,
        Money.of(new BigDecimal("50000.00")),
        true
      );
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.of(exceededRule));

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("Velocity"));
    }

    @Test
    void shouldReturnInvalidWhenVelocityLimitExceededByAmount() {
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(createActiveFeeConfig()));
      VelocityRule exceededRule = new VelocityRule(
        UUID.randomUUID(),
        VelocityScope.PER_AGENT,
        100,
        Money.of(new BigDecimal("50.00")),
        true
      );
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.of(exceededRule));

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("Velocity"));
    }

    @Test
    void shouldReturnInvalidWhenBothFeeConfigExpiredAndVelocityExceeded() {
      FeeConfig expiredConfig = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("1.50")),
        Money.of(new BigDecimal("0.50")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(60),
        LocalDate.now().minusDays(1)
      );
      VelocityRule exceededRule = new VelocityRule(
        UUID.randomUUID(),
        VelocityScope.PER_AGENT,
        1,
        Money.of(new BigDecimal("50000.00")),
        true
      );
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(expiredConfig));
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.of(exceededRule));

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).hasSize(2);
      assertThat(result.errors()).anyMatch(e -> e.contains("not active"));
      assertThat(result.errors()).anyMatch(e -> e.contains("Velocity"));
    }

    @Test
    void shouldReturnInvalidWhenAmountNotPositiveAndFeeConfigNotFound() {
      TransactionValidationRequest request = new TransactionValidationRequest(
        TransactionType.CASH_WITHDRAWAL,
        AgentId.of("AGT-001"),
        AgentType.STANDARD,
        Money.of(BigDecimal.ZERO),
        "123456789012"
      );
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.empty());
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.empty());

      ValidationResult result = rulesService.validate(request);

      assertThat(result.valid()).isFalse();
      assertThat(result.errors()).hasSize(2);
    }

    @Test
    void shouldReturnValidWhenVelocityRuleInactive() {
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(createActiveFeeConfig()));
      VelocityRule inactiveRule = new VelocityRule(
        UUID.randomUUID(),
        VelocityScope.PER_AGENT,
        1,
        Money.of(new BigDecimal("50.00")),
        false
      );
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.of(inactiveRule));

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isTrue();
      assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldCalculatePercentageFeeWhenConfigIsPercentageType() {
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(createPercentageFeeConfig()));
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.empty());

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isTrue();
      assertThat(result.feeCalculation()).isNotNull();
      assertThat(result.feeCalculation().isValid()).isTrue();
      assertThat(result.feeCalculation().customerFee()).isEqualTo(Money.of(new BigDecimal("1.50")));
    }

    @Test
    void shouldCalculateFixedFeeWhenConfigIsFixedType() {
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(any(), any()))
        .thenReturn(Optional.of(createActiveFeeConfig()));
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.empty());

      ValidationResult result = rulesService.validate(createValidRequest());

      assertThat(result.valid()).isTrue();
      assertThat(result.feeCalculation().customerFee()).isEqualTo(Money.of(new BigDecimal("1.50")));
      assertThat(result.feeCalculation().agentCommission()).isEqualTo(Money.of(new BigDecimal("0.50")));
      assertThat(result.feeCalculation().bankShare()).isEqualTo(Money.of(new BigDecimal("1.00")));
    }

    @Test
    void shouldHandleDifferentAgentTiers() {
      FeeConfig premierConfig = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.PREMIER,
        FeeType.FIXED,
        Money.of(new BigDecimal("0.50")),
        Money.of(new BigDecimal("0.25")),
        Money.of(new BigDecimal("0.25")),
        Money.of(new BigDecimal("50000.00")),
        200,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(30)
      );
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(
        TransactionType.CASH_WITHDRAWAL, AgentType.PREMIER))
        .thenReturn(Optional.of(premierConfig));
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.empty());

      TransactionValidationRequest request = new TransactionValidationRequest(
        TransactionType.CASH_WITHDRAWAL,
        AgentId.of("AGT-001"),
        AgentType.PREMIER,
        Money.of(new BigDecimal("100.00")),
        "123456789012"
      );

      ValidationResult result = rulesService.validate(request);

      assertThat(result.valid()).isTrue();
      assertThat(result.feeCalculation().customerFee()).isEqualTo(Money.of(new BigDecimal("0.50")));
    }

    @Test
    void shouldHandleDifferentTransactionTypes() {
      FeeConfig depositConfig = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_DEPOSIT,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("2.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(30)
      );
      when(feeConfigRepository.findByTransactionTypeAndAgentTier(
        TransactionType.CASH_DEPOSIT, AgentType.STANDARD))
        .thenReturn(Optional.of(depositConfig));
      when(velocityRuleRepository.findActiveByScope(any()))
        .thenReturn(Optional.empty());

      TransactionValidationRequest request = new TransactionValidationRequest(
        TransactionType.CASH_DEPOSIT,
        AgentId.of("AGT-001"),
        AgentType.STANDARD,
        Money.of(new BigDecimal("100.00")),
        "123456789012"
      );

      ValidationResult result = rulesService.validate(request);

      assertThat(result.valid()).isTrue();
      assertThat(result.feeCalculation().customerFee()).isEqualTo(Money.of(new BigDecimal("2.00")));
    }
  }

  @Nested
  class CalculateFee {

    @Test
    void shouldCalculateFixedFeeCorrectly() {
      FeeConfig config = createActiveFeeConfig();

      FeeCalculationResult result = feeCalculationService.calculate(config, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("1.50")));
      assertThat(result.agentCommission()).isEqualTo(Money.of(new BigDecimal("0.50")));
      assertThat(result.bankShare()).isEqualTo(Money.of(new BigDecimal("1.00")));
    }

    @Test
    void shouldCalculatePercentageFeeCorrectly() {
      FeeConfig config = createPercentageFeeConfig();

      FeeCalculationResult result = feeCalculationService.calculate(config, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("1.50")));
      assertThat(result.agentCommission()).isEqualTo(Money.of(new BigDecimal("0.50")));
      assertThat(result.bankShare()).isEqualTo(Money.of(new BigDecimal("1.00")));
    }

    @Test
    void shouldCalculatePercentageFeeForLargeAmounts() {
      FeeConfig config = createPercentageFeeConfig();

      FeeCalculationResult result = feeCalculationService.calculate(config, Money.of(new BigDecimal("10000.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("150.00")));
      assertThat(result.agentCommission()).isEqualTo(Money.of(new BigDecimal("50.00")));
      assertThat(result.bankShare()).isEqualTo(Money.of(new BigDecimal("100.00")));
    }

    @Test
    void shouldCalculatePercentageFeeForSmallAmounts() {
      FeeConfig config = createPercentageFeeConfig();

      FeeCalculationResult result = feeCalculationService.calculate(config, Money.of(new BigDecimal("10.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("0.15")));
      assertThat(result.agentCommission()).isEqualTo(Money.of(new BigDecimal("0.05")));
      assertThat(result.bankShare()).isEqualTo(Money.of(new BigDecimal("0.10")));
    }

    @Test
    void shouldReturnInvalidForExpiredConfig() {
      FeeConfig expiredConfig = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("1.50")),
        Money.of(new BigDecimal("0.50")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(60),
        LocalDate.now().minusDays(1)
      );

      FeeCalculationResult result = feeCalculationService.calculate(expiredConfig, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isFalse();
      assertThat(result.customerFee()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.agentCommission()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.bankShare()).isEqualTo(Money.of(BigDecimal.ZERO));
    }

    @Test
    void shouldReturnInvalidForNotYetEffectiveConfig() {
      FeeConfig futureConfig = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("1.50")),
        Money.of(new BigDecimal("0.50")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(60)
      );

      FeeCalculationResult result = feeCalculationService.calculate(futureConfig, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isFalse();
      assertThat(result.customerFee()).isEqualTo(Money.of(BigDecimal.ZERO));
    }

    @Test
    void shouldHandlePercentageWithRounding() {
      FeeConfig config = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.PERCENTAGE,
        Money.of(new BigDecimal("3.33")),
        Money.of(new BigDecimal("1.11")),
        Money.of(new BigDecimal("2.22")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(30)
      );

      FeeCalculationResult result = feeCalculationService.calculate(config, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("3.33")));
      assertThat(result.agentCommission()).isEqualTo(Money.of(new BigDecimal("1.11")));
      assertThat(result.bankShare()).isEqualTo(Money.of(new BigDecimal("2.22")));
    }

    @Test
    void shouldHandlePercentageWithFractionalResults() {
      FeeConfig config = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.PERCENTAGE,
        Money.of(new BigDecimal("1.5")),
        Money.of(new BigDecimal("0.5")),
        Money.of(new BigDecimal("1.0")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(30)
      );

      FeeCalculationResult result = feeCalculationService.calculate(config, Money.of(new BigDecimal("33.33")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("0.50")));
    }
  }
}
