package com.agentbanking.rules.domain.service;

import static org.assertj.core.api.Assertions.*;

import com.agentbanking.UnitTest;
import com.agentbanking.shared.onboarding.domain.AgentType;
import com.agentbanking.rules.domain.model.FeeCalculationResult;
import com.agentbanking.rules.domain.model.FeeConfig;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.shared.money.domain.Money;
import com.agentbanking.shared.transaction.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
@DisplayName("FeeCalculationService")
class FeeCalculationServiceTest {

  private FeeCalculationService service;

  @BeforeEach
  void setUp() {
    service = new FeeCalculationService();
  }

  private FeeConfig createFixedFeeConfig() {
    return new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
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
  }

  private FeeConfig createPercentageFeeConfig() {
    return new FeeConfig(
      UUID.randomUUID(),
      TransactionType.CASH_WITHDRAWAL,
      AgentType.STANDARD,
      FeeType.PERCENTAGE,
      Money.of(new BigDecimal("2.0")),
      Money.of(new BigDecimal("1.0")),
      Money.of(new BigDecimal("0.5")),
      Money.of(new BigDecimal("10000.00")),
      50,
      LocalDate.now().minusDays(1),
      LocalDate.now().plusDays(30)
    );
  }

  @Nested
  class CalculateFixedFee {

    @Test
    void shouldReturnFixedFeeValues() {
      FeeConfig config = createFixedFeeConfig();

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("500.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("2.00")));
      assertThat(result.agentCommission()).isEqualTo(Money.of(new BigDecimal("1.00")));
      assertThat(result.bankShare()).isEqualTo(Money.of(new BigDecimal("1.00")));
    }

    @Test
    void shouldReturnSameFixedFeeRegardlessOfTransactionAmount() {
      FeeConfig config = createFixedFeeConfig();

      FeeCalculationResult smallTx = service.calculate(config, Money.of(new BigDecimal("10.00")));
      FeeCalculationResult largeTx = service.calculate(config, Money.of(new BigDecimal("50000.00")));

      assertThat(smallTx.customerFee()).isEqualTo(Money.of(new BigDecimal("2.00")));
      assertThat(largeTx.customerFee()).isEqualTo(Money.of(new BigDecimal("2.00")));
      assertThat(smallTx.customerFee()).isEqualTo(largeTx.customerFee());
    }

    @Test
    void shouldHandleZeroFixedFee() {
      FeeConfig config = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.BALANCE_INQUIRY,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(BigDecimal.ZERO),
        Money.of(BigDecimal.ZERO),
        Money.of(BigDecimal.ZERO),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(30)
      );

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.agentCommission()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.bankShare()).isEqualTo(Money.of(BigDecimal.ZERO));
    }

    @Test
    void shouldHandleDifferentAgentTiersWithDifferentFixedFees() {
      FeeConfig microConfig = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.MICRO,
        FeeType.FIXED,
        Money.of(new BigDecimal("3.00")),
        Money.of(new BigDecimal("1.50")),
        Money.of(new BigDecimal("1.50")),
        Money.of(new BigDecimal("5000.00")),
        20,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(30)
      );
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

      FeeCalculationResult microResult = service.calculate(microConfig, Money.of(new BigDecimal("100.00")));
      FeeCalculationResult premierResult = service.calculate(premierConfig, Money.of(new BigDecimal("100.00")));

      assertThat(microResult.customerFee()).isEqualTo(Money.of(new BigDecimal("3.00")));
      assertThat(premierResult.customerFee()).isEqualTo(Money.of(new BigDecimal("0.50")));
    }
  }

  @Nested
  class CalculatePercentageFee {

    @Test
    void shouldCalculatePercentageBasedFee() {
      FeeConfig config = createPercentageFeeConfig();

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("1000.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("20.00")));
      assertThat(result.agentCommission()).isEqualTo(Money.of(new BigDecimal("10.00")));
      assertThat(result.bankShare()).isEqualTo(Money.of(new BigDecimal("5.00")));
    }

    @Test
    void shouldHandleSmallAmountsCorrectly() {
      FeeConfig config = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.MICRO,
        FeeType.PERCENTAGE,
        Money.of(new BigDecimal("1.0")),
        Money.of(new BigDecimal("0.5")),
        Money.of(new BigDecimal("0.5")),
        Money.of(new BigDecimal("5000.00")),
        20,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(30)
      );

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("10.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("0.10")));
    }

    @Test
    void shouldHandleLargeAmountsCorrectly() {
      FeeConfig config = createPercentageFeeConfig();

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("50000.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("1000.00")));
      assertThat(result.agentCommission()).isEqualTo(Money.of(new BigDecimal("500.00")));
      assertThat(result.bankShare()).isEqualTo(Money.of(new BigDecimal("250.00")));
    }

    @Test
    void shouldRoundHalfUpForFractionalCents() {
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

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("33.33")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("0.50")));
    }

    @Test
    void shouldHandleOnePercentFee() {
      FeeConfig config = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.PERCENTAGE,
        Money.of(new BigDecimal("1.0")),
        Money.of(new BigDecimal("0.5")),
        Money.of(new BigDecimal("0.5")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(30)
      );

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("1.00")));
      assertThat(result.agentCommission()).isEqualTo(Money.of(new BigDecimal("0.50")));
      assertThat(result.bankShare()).isEqualTo(Money.of(new BigDecimal("0.50")));
    }

    @Test
    void shouldHandleZeroPercentageFee() {
      FeeConfig config = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.PERCENTAGE,
        Money.of(BigDecimal.ZERO),
        Money.of(BigDecimal.ZERO),
        Money.of(BigDecimal.ZERO),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(1),
        LocalDate.now().plusDays(30)
      );

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("1000.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(BigDecimal.ZERO));
    }
  }

  @Nested
  class InactiveConfig {

    @Test
    void shouldReturnInvalidForExpiredConfig() {
      FeeConfig expiredConfig = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("2.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(60),
        LocalDate.now().minusDays(1)
      );

      FeeCalculationResult result = service.calculate(expiredConfig, Money.of(new BigDecimal("100.00")));

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
        Money.of(new BigDecimal("2.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().plusDays(1),
        LocalDate.now().plusDays(60)
      );

      FeeCalculationResult result = service.calculate(futureConfig, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isFalse();
      assertThat(result.customerFee()).isEqualTo(Money.of(BigDecimal.ZERO));
    }

    @Test
    void shouldReturnInvalidForExpiredPercentageConfig() {
      FeeConfig expiredConfig = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.PERCENTAGE,
        Money.of(new BigDecimal("2.0")),
        Money.of(new BigDecimal("1.0")),
        Money.of(new BigDecimal("0.5")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(60),
        LocalDate.now().minusDays(1)
      );

      FeeCalculationResult result = service.calculate(expiredConfig, Money.of(new BigDecimal("1000.00")));

      assertThat(result.isValid()).isFalse();
      assertThat(result.customerFee()).isEqualTo(Money.of(BigDecimal.ZERO));
    }
  }

  @Nested
  class BoundaryConditions {

    @Test
    void shouldHandleConfigEffectiveToday() {
      FeeConfig config = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("2.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now(),
        LocalDate.now().plusDays(30)
      );

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("2.00")));
    }

    @Test
    void shouldHandleConfigExpiringToday() {
      FeeConfig config = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("2.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(30),
        LocalDate.now()
      );

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("2.00")));
    }

    @Test
    void shouldHandleConfigWithNullEffectiveTo() {
      FeeConfig config = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("2.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        LocalDate.now().minusDays(1),
        null
      );

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("2.00")));
    }

    @Test
    void shouldHandleConfigWithNullEffectiveFrom() {
      FeeConfig config = new FeeConfig(
        UUID.randomUUID(),
        TransactionType.CASH_WITHDRAWAL,
        AgentType.STANDARD,
        FeeType.FIXED,
        Money.of(new BigDecimal("2.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("1.00")),
        Money.of(new BigDecimal("10000.00")),
        50,
        null,
        LocalDate.now().plusDays(30)
      );

      FeeCalculationResult result = service.calculate(config, Money.of(new BigDecimal("100.00")));

      assertThat(result.isValid()).isTrue();
      assertThat(result.customerFee()).isEqualTo(Money.of(new BigDecimal("2.00")));
    }
  }
}
