package com.agentbanking.floatagg.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agentbanking.UnitTest;
import com.agentbanking.floatagg.domain.model.AgentFloat;
import com.agentbanking.floatagg.domain.port.in.BalanceInquiryUseCase.BalanceResult;
import com.agentbanking.floatagg.domain.port.in.CreditFloatUseCase.CreditResult;
import com.agentbanking.floatagg.domain.port.in.DebitFloatUseCase.DebitResult;
import com.agentbanking.floatagg.domain.port.in.LockFloatUseCase.LockResult;
import com.agentbanking.floatagg.domain.port.out.AgentFloatRepository;
import com.agentbanking.shared.identity.domain.AgentId;
import com.agentbanking.shared.money.domain.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
@DisplayName("FloatService")
class FloatServiceTest {

  @Mock
  private AgentFloatRepository agentFloatRepository;

  private FloatService floatService;

  private static final AgentId AGENT_ID = AgentId.of("AGT-001");
  private static final Currency MYR = Currency.getInstance("MYR");

  @BeforeEach
  void setUp() {
    floatService = new FloatService(agentFloatRepository);
  }

  private AgentFloat createAgentFloat(Money balance, Money reserved, Money available) {
    return new AgentFloat(AGENT_ID, balance, reserved, available, MYR, Instant.now());
  }

  @Nested
  class ReserveFloat {

    @Test
    void shouldReserveFloatSuccessfully() {
      Money amount = Money.of(new BigDecimal("100.00"));
      AgentFloat floatAccount = createAgentFloat(
        Money.of(new BigDecimal("500.00")),
        Money.of(new BigDecimal("0.00")),
        Money.of(new BigDecimal("500.00"))
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));
      when(agentFloatRepository.save(any(AgentFloat.class))).thenAnswer(invocation -> invocation.getArgument(0));

      LockResult result = floatService.reserveFloat(AGENT_ID, amount, "SAGA-001");

      assertThat(result.success()).isTrue();
      assertThat(result.lockId()).startsWith("LOCK-");
      assertThat(result.errorCode()).isNull();
      verify(agentFloatRepository).save(argThat(saved ->
        saved.availableBalance().equals(Money.of(new BigDecimal("400.00"))) &&
        saved.reservedBalance().equals(Money.of(new BigDecimal("100.00")))
      ));
    }

    @Test
    void shouldReturnErrorWhenAgentNotFound() {
      Money amount = Money.of(new BigDecimal("100.00"));
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.empty());

      LockResult result = floatService.reserveFloat(AGENT_ID, amount, "SAGA-001");

      assertThat(result.success()).isFalse();
      assertThat(result.lockId()).isNull();
      assertThat(result.errorCode()).isEqualTo("ERR_BIZ_206");
      verify(agentFloatRepository, never()).save(any());
    }

    @Test
    void shouldReturnErrorWhenInsufficientBalance() {
      Money amount = Money.of(new BigDecimal("600.00"));
      AgentFloat floatAccount = createAgentFloat(
        Money.of(new BigDecimal("500.00")),
        Money.of(new BigDecimal("0.00")),
        Money.of(new BigDecimal("500.00"))
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));

      LockResult result = floatService.reserveFloat(AGENT_ID, amount, "SAGA-001");

      assertThat(result.success()).isFalse();
      assertThat(result.lockId()).isNull();
      assertThat(result.errorCode()).isEqualTo("ERR_BIZ_201");
      verify(agentFloatRepository, never()).save(any());
    }

    @Test
    void shouldReserveWhenAmountEqualsAvailableBalance() {
      Money amount = Money.of(new BigDecimal("500.00"));
      AgentFloat floatAccount = createAgentFloat(
        Money.of(new BigDecimal("500.00")),
        Money.of(new BigDecimal("0.00")),
        Money.of(new BigDecimal("500.00"))
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));
      when(agentFloatRepository.save(any(AgentFloat.class))).thenAnswer(invocation -> invocation.getArgument(0));

      LockResult result = floatService.reserveFloat(AGENT_ID, amount, "SAGA-001");

      assertThat(result.success()).isTrue();
      assertThat(result.errorCode()).isNull();
      verify(agentFloatRepository).save(argThat(saved ->
        saved.availableBalance().equals(Money.of(new BigDecimal("0.00"))) &&
        saved.reservedBalance().equals(Money.of(new BigDecimal("500.00")))
      ));
    }

    @Test
    void shouldFailWhenReservingZeroAmount() {
      Money amount = Money.of(BigDecimal.ZERO);
      AgentFloat floatAccount = createAgentFloat(
        Money.of(new BigDecimal("500.00")),
        Money.of(new BigDecimal("0.00")),
        Money.of(new BigDecimal("500.00"))
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));
      when(agentFloatRepository.save(any(AgentFloat.class))).thenAnswer(invocation -> invocation.getArgument(0));

      LockResult result = floatService.reserveFloat(AGENT_ID, amount, "SAGA-001");

      assertThat(result.success()).isTrue();
      verify(agentFloatRepository).save(argThat(saved ->
        saved.availableBalance().equals(Money.of(new BigDecimal("500.00"))) &&
        saved.reservedBalance().equals(Money.of(new BigDecimal("0.00")))
      ));
    }
  }

  @Nested
  class DebitFloat {

    @Test
    void shouldDebitFloatSuccessfully() {
      Money amount = Money.of(new BigDecimal("100.00"));
      AgentFloat floatAccount = createAgentFloat(
        Money.of(new BigDecimal("500.00")),
        Money.of(new BigDecimal("0.00")),
        Money.of(new BigDecimal("500.00"))
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));
      when(agentFloatRepository.save(any(AgentFloat.class))).thenAnswer(invocation -> invocation.getArgument(0));

      DebitResult result = floatService.debit(AGENT_ID, amount, "LOCK-001");

      assertThat(result.success()).isTrue();
      assertThat(result.debitId()).startsWith("DEBIT-");
      assertThat(result.errorCode()).isNull();
      verify(agentFloatRepository).save(argThat(saved ->
        saved.balance().equals(Money.of(new BigDecimal("400.00"))) &&
        saved.availableBalance().equals(Money.of(new BigDecimal("400.00")))
      ));
    }

    @Test
    void shouldReturnErrorWhenAgentNotFound() {
      Money amount = Money.of(new BigDecimal("100.00"));
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.empty());

      DebitResult result = floatService.debit(AGENT_ID, amount, "LOCK-001");

      assertThat(result.success()).isFalse();
      assertThat(result.debitId()).isNull();
      assertThat(result.errorCode()).isEqualTo("ERR_BIZ_206");
      verify(agentFloatRepository, never()).save(any());
    }

    @Test
    void shouldDebitExactBalance() {
      Money amount = Money.of(new BigDecimal("500.00"));
      AgentFloat floatAccount = createAgentFloat(
        Money.of(new BigDecimal("500.00")),
        Money.of(new BigDecimal("0.00")),
        Money.of(new BigDecimal("500.00"))
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));
      when(agentFloatRepository.save(any(AgentFloat.class))).thenAnswer(invocation -> invocation.getArgument(0));

      DebitResult result = floatService.debit(AGENT_ID, amount, "LOCK-001");

      assertThat(result.success()).isTrue();
      verify(agentFloatRepository).save(argThat(saved ->
        saved.balance().equals(Money.of(BigDecimal.ZERO)) &&
        saved.availableBalance().equals(Money.of(BigDecimal.ZERO))
      ));
    }

    @Test
    void shouldDebitZeroAmount() {
      Money amount = Money.of(BigDecimal.ZERO);
      AgentFloat floatAccount = createAgentFloat(
        Money.of(new BigDecimal("500.00")),
        Money.of(new BigDecimal("0.00")),
        Money.of(new BigDecimal("500.00"))
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));
      when(agentFloatRepository.save(any(AgentFloat.class))).thenAnswer(invocation -> invocation.getArgument(0));

      DebitResult result = floatService.debit(AGENT_ID, amount, "LOCK-001");

      assertThat(result.success()).isTrue();
      verify(agentFloatRepository).save(argThat(saved ->
        saved.balance().equals(Money.of(new BigDecimal("500.00")))
      ));
    }
  }

  @Nested
  class CreditFloat {

    @Test
    void shouldCreditExistingFloat() {
      Money amount = Money.of(new BigDecimal("100.00"));
      AgentFloat floatAccount = createAgentFloat(
        Money.of(new BigDecimal("500.00")),
        Money.of(new BigDecimal("0.00")),
        Money.of(new BigDecimal("500.00"))
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));
      when(agentFloatRepository.save(any(AgentFloat.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CreditResult result = floatService.credit(AGENT_ID, amount);

      assertThat(result.success()).isTrue();
      assertThat(result.creditId()).startsWith("CREDIT-");
      assertThat(result.errorCode()).isNull();
      verify(agentFloatRepository).save(argThat(saved ->
        saved.balance().equals(Money.of(new BigDecimal("600.00"))) &&
        saved.availableBalance().equals(Money.of(new BigDecimal("600.00")))
      ));
    }

    @Test
    void shouldCreateNewFloatWhenAgentNotFound() {
      Money amount = Money.of(new BigDecimal("100.00"));
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.empty());
      when(agentFloatRepository.save(any(AgentFloat.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CreditResult result = floatService.credit(AGENT_ID, amount);

      assertThat(result.success()).isTrue();
      assertThat(result.creditId()).startsWith("CREDIT-");
      assertThat(result.errorCode()).isNull();
      verify(agentFloatRepository).save(argThat(saved ->
        saved.agentId().equals(AGENT_ID) &&
        saved.balance().equals(Money.of(new BigDecimal("100.00"))) &&
        saved.availableBalance().equals(Money.of(new BigDecimal("100.00"))) &&
        saved.reservedBalance().equals(Money.of(BigDecimal.ZERO)) &&
        saved.currency().equals(MYR)
      ));
    }

    @Test
    void shouldCreditZeroAmount() {
      Money amount = Money.of(BigDecimal.ZERO);
      AgentFloat floatAccount = createAgentFloat(
        Money.of(new BigDecimal("500.00")),
        Money.of(new BigDecimal("0.00")),
        Money.of(new BigDecimal("500.00"))
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));
      when(agentFloatRepository.save(any(AgentFloat.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CreditResult result = floatService.credit(AGENT_ID, amount);

      assertThat(result.success()).isTrue();
      verify(agentFloatRepository).save(argThat(saved ->
        saved.balance().equals(Money.of(new BigDecimal("500.00")))
      ));
    }
  }

  @Nested
  class GetBalance {

    @Test
    void shouldReturnCorrectBalances() {
      AgentFloat floatAccount = createAgentFloat(
        Money.of(new BigDecimal("1000.00")),
        Money.of(new BigDecimal("200.00")),
        Money.of(new BigDecimal("800.00"))
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));

      BalanceResult result = floatService.getBalance(AGENT_ID);

      assertThat(result.balance()).isEqualTo(Money.of(new BigDecimal("1000.00")));
      assertThat(result.availableBalance()).isEqualTo(Money.of(new BigDecimal("800.00")));
      assertThat(result.reservedBalance()).isEqualTo(Money.of(new BigDecimal("200.00")));
      assertThat(result.currency()).isEqualTo("MYR");
    }

    @Test
    void shouldReturnZerosWhenAgentNotFound() {
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.empty());

      BalanceResult result = floatService.getBalance(AGENT_ID);

      assertThat(result.balance()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.availableBalance()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.reservedBalance()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.currency()).isEqualTo("MYR");
    }

    @Test
    void shouldReturnBalancesWithAllZeroValues() {
      AgentFloat floatAccount = createAgentFloat(
        Money.of(BigDecimal.ZERO),
        Money.of(BigDecimal.ZERO),
        Money.of(BigDecimal.ZERO)
      );
      when(agentFloatRepository.findByAgentId(AGENT_ID)).thenReturn(Optional.of(floatAccount));

      BalanceResult result = floatService.getBalance(AGENT_ID);

      assertThat(result.balance()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.availableBalance()).isEqualTo(Money.of(BigDecimal.ZERO));
      assertThat(result.reservedBalance()).isEqualTo(Money.of(BigDecimal.ZERO));
    }
  }
}
