package com.agentbanking.commission.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agentbanking.UnitTest;
import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionStatus;
import com.agentbanking.commission.domain.model.CommissionType;
import com.agentbanking.commission.domain.port.out.CommissionRepository;
import com.agentbanking.shared.money.domain.Money;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
@DisplayName("CommissionCalculationService")
class CommissionCalculationServiceTest {

  @Mock
  private CommissionRepository commissionRepository;

  @Captor
  private ArgumentCaptor<CommissionEntry> commissionEntryCaptor;

  private CommissionCalculationService commissionCalculationService;

  private static final String TRANSACTION_ID = "TXN-001";
  private static final String AGENT_ID = "AGT-001";

  @BeforeEach
  void setUp() {
    commissionCalculationService = new CommissionCalculationService(commissionRepository);
  }

  @Nested
  class CalculateCommission {

    @Test
    void shouldCalculateWithdrawalCommissionAtOnePercent() {
      Money amount = Money.of(new BigDecimal("1000.00"));
      Money expectedCommission = Money.of(new BigDecimal("10.00"));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommissionEntry result = commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "CASH_WITHDRAWAL", amount);

      assertThat(result.transactionId()).isEqualTo(TRANSACTION_ID);
      assertThat(result.agentId()).isEqualTo(AGENT_ID);
      assertThat(result.type()).isEqualTo(CommissionType.CASH_WITHDRAWAL);
      assertThat(result.transactionAmount()).isEqualTo(amount);
      assertThat(result.commissionAmount()).isEqualTo(expectedCommission);
      assertThat(result.status()).isEqualTo(CommissionStatus.CALCULATED);
      assertThat(result.id()).isNotNull();
      assertThat(result.createdAt()).isNotNull();
      assertThat(result.settledAt()).isNull();
    }

    @Test
    void shouldCalculateDepositCommissionAtHalfPercent() {
      Money amount = Money.of(new BigDecimal("2000.00"));
      Money expectedCommission = Money.of(new BigDecimal("10.00"));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommissionEntry result = commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "CASH_DEPOSIT", amount);

      assertThat(result.commissionAmount()).isEqualTo(expectedCommission);
      assertThat(result.type()).isEqualTo(CommissionType.CASH_DEPOSIT);
    }

    @Test
    void shouldCalculateBillPaymentCommissionAtZeroPointEightPercent() {
      Money amount = Money.of(new BigDecimal("500.00"));
      Money expectedCommission = Money.of(new BigDecimal("4.00"));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommissionEntry result = commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "BILL_PAYMENT", amount);

      assertThat(result.commissionAmount()).isEqualTo(expectedCommission);
      assertThat(result.type()).isEqualTo(CommissionType.BILL_PAYMENT);
    }

    @Test
    void shouldCalculateCommissionForSmallAmount() {
      Money amount = Money.of(new BigDecimal("1.00"));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommissionEntry result = commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "CASH_WITHDRAWAL", amount);

      assertThat(result.commissionAmount()).isEqualTo(Money.of(new BigDecimal("0.01")));
    }

    @Test
    void shouldCalculateCommissionForLargeAmount() {
      Money amount = Money.of(new BigDecimal("999999.99"));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommissionEntry result = commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "CASH_WITHDRAWAL", amount);

      assertThat(result.commissionAmount()).isEqualTo(Money.of(new BigDecimal("10000.00")));
    }

    @Test
    void shouldCalculateCommissionForZeroAmount() {
      Money amount = Money.of(BigDecimal.ZERO);
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommissionEntry result = commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "CASH_DEPOSIT", amount);

      assertThat(result.commissionAmount()).isEqualTo(Money.of(BigDecimal.ZERO));
    }

    @Test
    void shouldCalculateCommissionWithRounding() {
      Money amount = Money.of(new BigDecimal("333.33"));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommissionEntry result = commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "CASH_WITHDRAWAL", amount);

      assertThat(result.commissionAmount()).isEqualTo(Money.of(new BigDecimal("3.33")));
    }

    @Test
    void shouldSaveEntryToRepository() {
      Money amount = Money.of(new BigDecimal("100.00"));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "CASH_WITHDRAWAL", amount);

      verify(commissionRepository).save(commissionEntryCaptor.capture());
      CommissionEntry captured = commissionEntryCaptor.getValue();
      assertThat(captured.type()).isEqualTo(CommissionType.CASH_WITHDRAWAL);
      assertThat(captured.status()).isEqualTo(CommissionStatus.CALCULATED);
    }

    @Test
    void shouldReturnSavedEntryFromRepository() {
      Money amount = Money.of(new BigDecimal("100.00"));
      CommissionEntry savedEntry = new CommissionEntry(
        java.util.UUID.randomUUID(),
        TRANSACTION_ID,
        AGENT_ID,
        CommissionType.CASH_WITHDRAWAL,
        amount,
        Money.of(new BigDecimal("1.00")),
        CommissionStatus.CALCULATED,
        java.time.Instant.now(),
        null
      );
      when(commissionRepository.save(any(CommissionEntry.class))).thenReturn(savedEntry);

      CommissionEntry result = commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "CASH_WITHDRAWAL", amount);

      assertThat(result).isSameAs(savedEntry);
    }

    @Test
    void shouldCalculateWithdrawalCommissionForFiftyAmount() {
      Money amount = Money.of(new BigDecimal("50.00"));
      Money expectedCommission = Money.of(new BigDecimal("0.50"));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommissionEntry result = commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "CASH_WITHDRAWAL", amount);

      assertThat(result.commissionAmount()).isEqualTo(expectedCommission);
    }

    @Test
    void shouldCalculateBillPaymentCommissionForLargeAmount() {
      Money amount = Money.of(new BigDecimal("10000.00"));
      Money expectedCommission = Money.of(new BigDecimal("80.00"));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      CommissionEntry result = commissionCalculationService.calculate(TRANSACTION_ID, AGENT_ID, "BILL_PAYMENT", amount);

      assertThat(result.commissionAmount()).isEqualTo(expectedCommission);
    }
  }
}
