package com.agentbanking.floatagg.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;

@UnitTest
class AgentFloatTest {

  @Nested
  @DisplayName("hasSufficientBalance")
  class HasSufficientBalanceTest {

    @Test
    void shouldHaveSufficientBalance() {
      AgentFloat floatAccount = anAgentFloatWithBalance("5000.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      assertThat(floatAccount.hasSufficientBalance(amount)).isTrue();
    }

    @Test
    void shouldNotHaveSufficientBalance() {
      AgentFloat floatAccount = anAgentFloatWithBalance("500.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      assertThat(floatAccount.hasSufficientBalance(amount)).isFalse();
    }
  }

  @Nested
  @DisplayName("debit")
  class DebitTest {

    @Test
    void shouldDebitFromBalance() {
      AgentFloat floatAccount = anAgentFloatWithBalance("5000.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      AgentFloat debited = floatAccount.debit(amount);

      assertThat(debited.balance().amount())
        .isEqualByComparingTo(new BigDecimal("4000.00"));
      assertThat(debited.availableBalance().amount())
        .isEqualByComparingTo(new BigDecimal("4000.00"));
    }
  }

  @Nested
  @DisplayName("credit")
  class CreditTest {

    @Test
    void shouldCreditToBalance() {
      AgentFloat floatAccount = anAgentFloatWithBalance("5000.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      AgentFloat credited = floatAccount.credit(amount);

      assertThat(credited.balance().amount())
        .isEqualByComparingTo(new BigDecimal("6000.00"));
      assertThat(credited.availableBalance().amount())
        .isEqualByComparingTo(new BigDecimal("6000.00"));
    }
  }

  @Nested
  @DisplayName("reserve")
  class ReserveTest {

    @Test
    void shouldReserveAmount() {
      AgentFloat floatAccount = anAgentFloatWithBalance("5000.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      AgentFloat reserved = floatAccount.reserve(amount);

      assertThat(reserved.reservedBalance().amount())
        .isEqualByComparingTo(new BigDecimal("1000.00"));
      assertThat(reserved.availableBalance().amount())
        .isEqualByComparingTo(new BigDecimal("4000.00"));
    }
  }

  @Nested
  @DisplayName("release")
  class ReleaseTest {

    @Test
    void shouldReleaseReservedAmount() {
      AgentFloat floatAccount = anAgentFloatWithBalanceAndReserved("5000.00", "1000.00");
      Money amount = Money.of(new BigDecimal("1000.00"));

      AgentFloat released = floatAccount.release(amount);

      assertThat(released.reservedBalance().amount())
        .isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(released.availableBalance().amount())
        .isEqualByComparingTo(new BigDecimal("5000.00"));
    }
  }

  private AgentFloat anAgentFloatWithBalance(String balance) {
    return new AgentFloat(
      AgentId.of("AGT-001"),
      Money.of(new BigDecimal(balance)),
      Money.of(BigDecimal.ZERO),
      Money.of(new BigDecimal(balance)),
      Currency.getInstance("MYR"),
      Instant.now()
    );
  }

  private AgentFloat anAgentFloatWithBalanceAndReserved(String balance, String reserved) {
    Money balanceMoney = Money.of(new BigDecimal(balance));
    Money reservedMoney = Money.of(new BigDecimal(reserved));
    Money availableMoney = Money.of(
      new BigDecimal(balance).subtract(new BigDecimal(reserved))
    );
    return new AgentFloat(
      AgentId.of("AGT-001"),
      balanceMoney,
      reservedMoney,
      availableMoney,
      Currency.getInstance("MYR"),
      Instant.now()
    );
  }
}
