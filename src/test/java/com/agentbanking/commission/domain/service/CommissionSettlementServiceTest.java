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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
@DisplayName("CommissionSettlementService")
class CommissionSettlementServiceTest {

  @Mock
  private CommissionRepository commissionRepository;

  @Captor
  private ArgumentCaptor<CommissionEntry> commissionEntryCaptor;

  private CommissionSettlementService commissionSettlementService;

  @BeforeEach
  void setUp() {
    commissionSettlementService = new CommissionSettlementService(commissionRepository);
  }

  @Nested
  class SettlePendingCommissions {

    @Test
    void shouldSettleAllPendingCommissions() {
      CommissionEntry entry1 = createPendingEntry("TXN-001", "AGT-001", CommissionType.CASH_WITHDRAWAL, new BigDecimal("1000.00"), new BigDecimal("10.00"));
      CommissionEntry entry2 = createPendingEntry("TXN-002", "AGT-002", CommissionType.CASH_DEPOSIT, new BigDecimal("2000.00"), new BigDecimal("10.00"));
      when(commissionRepository.findBySettledAtIsNull()).thenReturn(List.of(entry1, entry2));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      int result = commissionSettlementService.settlePendingCommissions();

      assertThat(result).isEqualTo(2);
      verify(commissionRepository, times(2)).save(commissionEntryCaptor.capture());
      List<CommissionEntry> savedEntries = commissionEntryCaptor.getAllValues();
      assertThat(savedEntries).allSatisfy(entry -> {
        assertThat(entry.status()).isEqualTo(CommissionStatus.SETTLED);
        assertThat(entry.settledAt()).isNotNull();
      });
    }

    @Test
    void shouldReturnZeroWhenNoPendingCommissions() {
      when(commissionRepository.findBySettledAtIsNull()).thenReturn(List.of());

      int result = commissionSettlementService.settlePendingCommissions();

      assertThat(result).isZero();
      verify(commissionRepository, never()).save(any());
    }

    @Test
    void shouldSettleSinglePendingCommission() {
      CommissionEntry entry = createPendingEntry("TXN-001", "AGT-001", CommissionType.BILL_PAYMENT, new BigDecimal("500.00"), new BigDecimal("4.00"));
      when(commissionRepository.findBySettledAtIsNull()).thenReturn(List.of(entry));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      int result = commissionSettlementService.settlePendingCommissions();

      assertThat(result).isEqualTo(1);
      verify(commissionRepository).save(commissionEntryCaptor.capture());
      CommissionEntry saved = commissionEntryCaptor.getValue();
      assertThat(saved.status()).isEqualTo(CommissionStatus.SETTLED);
      assertThat(saved.settledAt()).isNotNull();
    }

    @Test
    void shouldPreserveOriginalEntryDataWhenSettling() {
      Instant createdAt = Instant.now().minusSeconds(3600);
      CommissionEntry entry = new CommissionEntry(
        UUID.randomUUID(),
        "TXN-001",
        "AGT-001",
        CommissionType.CASH_WITHDRAWAL,
        Money.of(new BigDecimal("1000.00")),
        Money.of(new BigDecimal("10.00")),
        CommissionStatus.CALCULATED,
        createdAt,
        null
      );
      when(commissionRepository.findBySettledAtIsNull()).thenReturn(List.of(entry));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      commissionSettlementService.settlePendingCommissions();

      verify(commissionRepository).save(commissionEntryCaptor.capture());
      CommissionEntry saved = commissionEntryCaptor.getValue();
      assertThat(saved.id()).isEqualTo(entry.id());
      assertThat(saved.transactionId()).isEqualTo(entry.transactionId());
      assertThat(saved.agentId()).isEqualTo(entry.agentId());
      assertThat(saved.type()).isEqualTo(entry.type());
      assertThat(saved.transactionAmount()).isEqualTo(entry.transactionAmount());
      assertThat(saved.commissionAmount()).isEqualTo(entry.commissionAmount());
      assertThat(saved.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldSettleMultipleCommissionsForDifferentTypes() {
      CommissionEntry withdrawal = createPendingEntry("TXN-001", "AGT-001", CommissionType.CASH_WITHDRAWAL, new BigDecimal("1000.00"), new BigDecimal("10.00"));
      CommissionEntry deposit = createPendingEntry("TXN-002", "AGT-001", CommissionType.CASH_DEPOSIT, new BigDecimal("5000.00"), new BigDecimal("25.00"));
      CommissionEntry billPayment = createPendingEntry("TXN-003", "AGT-002", CommissionType.BILL_PAYMENT, new BigDecimal("200.00"), new BigDecimal("1.60"));
      when(commissionRepository.findBySettledAtIsNull()).thenReturn(List.of(withdrawal, deposit, billPayment));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      int result = commissionSettlementService.settlePendingCommissions();

      assertThat(result).isEqualTo(3);
    }

    @Test
    void shouldSettleCommissionsForMultipleAgents() {
      CommissionEntry entry1 = createPendingEntry("TXN-001", "AGT-001", CommissionType.CASH_WITHDRAWAL, new BigDecimal("100.00"), new BigDecimal("1.00"));
      CommissionEntry entry2 = createPendingEntry("TXN-002", "AGT-002", CommissionType.CASH_DEPOSIT, new BigDecimal("200.00"), new BigDecimal("1.00"));
      CommissionEntry entry3 = createPendingEntry("TXN-003", "AGT-003", CommissionType.BILL_PAYMENT, new BigDecimal("300.00"), new BigDecimal("2.40"));
      when(commissionRepository.findBySettledAtIsNull()).thenReturn(List.of(entry1, entry2, entry3));
      when(commissionRepository.save(any(CommissionEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

      int result = commissionSettlementService.settlePendingCommissions();

      assertThat(result).isEqualTo(3);
      verify(commissionRepository, times(3)).save(any());
    }
  }

  private CommissionEntry createPendingEntry(String txnId, String agentId, CommissionType type, BigDecimal txnAmount, BigDecimal commissionAmount) {
    return new CommissionEntry(
      UUID.randomUUID(),
      txnId,
      agentId,
      type,
      Money.of(txnAmount),
      Money.of(commissionAmount),
      CommissionStatus.CALCULATED,
      Instant.now().minusSeconds(3600),
      null
    );
  }
}
