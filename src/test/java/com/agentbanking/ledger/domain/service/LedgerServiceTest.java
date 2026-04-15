package com.agentbanking.ledger.domain.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.agentbanking.UnitTest;
import com.agentbanking.ledger.domain.model.AccountType;
import com.agentbanking.ledger.domain.model.EntryType;
import com.agentbanking.ledger.domain.model.LedgerEntry;
import com.agentbanking.ledger.domain.port.out.LedgerRepository;
import com.agentbanking.shared.money.domain.Money;
import java.math.BigDecimal;
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
@DisplayName("LedgerService")
class LedgerServiceTest {

  @Mock
  private LedgerRepository ledgerRepository;

  @Captor
  private ArgumentCaptor<LedgerEntry> ledgerEntryCaptor;

  private LedgerService ledgerService;

  private static final String TRANSACTION_ID = "TXN-001";
  private static final String DEBIT_ACCOUNT = "ACC-DEBIT-001";
  private static final String CREDIT_ACCOUNT = "ACC-CREDIT-001";
  private static final String DESCRIPTION = "Test transaction";

  @BeforeEach
  void setUp() {
    ledgerService = new LedgerService(ledgerRepository);
  }

  @Nested
  class RecordTransaction {

    @Test
    void shouldCreateDebitAndCreditEntries() {
      Money amount = Money.of(new BigDecimal("100.00"));
      when(ledgerRepository.createEntry(any(LedgerEntry.class))).thenReturn(UUID.randomUUID());

      ledgerService.recordTransaction(TRANSACTION_ID, amount, DEBIT_ACCOUNT, CREDIT_ACCOUNT, DESCRIPTION);

      verify(ledgerRepository, times(2)).createEntry(ledgerEntryCaptor.capture());
      List<LedgerEntry> capturedEntries = ledgerEntryCaptor.getAllValues();

      assertThat(capturedEntries).hasSize(2);

      LedgerEntry debitEntry = capturedEntries.get(0);
      assertThat(debitEntry.transactionId()).isEqualTo(TRANSACTION_ID);
      assertThat(debitEntry.accountCode()).isEqualTo(DEBIT_ACCOUNT);
      assertThat(debitEntry.accountType()).isEqualTo(AccountType.ASSET);
      assertThat(debitEntry.entryType()).isEqualTo(EntryType.DEBIT);
      assertThat(debitEntry.amount()).isEqualTo(amount);
      assertThat(debitEntry.description()).isEqualTo(DESCRIPTION);
      assertThat(debitEntry.id()).isNotNull();
      assertThat(debitEntry.createdAt()).isNotNull();

      LedgerEntry creditEntry = capturedEntries.get(1);
      assertThat(creditEntry.transactionId()).isEqualTo(TRANSACTION_ID);
      assertThat(creditEntry.accountCode()).isEqualTo(CREDIT_ACCOUNT);
      assertThat(creditEntry.accountType()).isEqualTo(AccountType.ASSET);
      assertThat(creditEntry.entryType()).isEqualTo(EntryType.CREDIT);
      assertThat(creditEntry.amount()).isEqualTo(amount);
      assertThat(creditEntry.description()).isEqualTo(DESCRIPTION);
      assertThat(creditEntry.id()).isNotNull();
      assertThat(creditEntry.createdAt()).isNotNull();
    }

    @Test
    void shouldCreateEntriesWithUniqueIds() {
      Money amount = Money.of(new BigDecimal("50.00"));
      when(ledgerRepository.createEntry(any(LedgerEntry.class))).thenReturn(UUID.randomUUID());

      ledgerService.recordTransaction(TRANSACTION_ID, amount, DEBIT_ACCOUNT, CREDIT_ACCOUNT, DESCRIPTION);

      verify(ledgerRepository, times(2)).createEntry(ledgerEntryCaptor.capture());
      List<LedgerEntry> capturedEntries = ledgerEntryCaptor.getAllValues();

      assertThat(capturedEntries.get(0).id()).isNotEqualTo(capturedEntries.get(1).id());
    }

    @Test
    void shouldCreateEntriesWithZeroAmount() {
      Money amount = Money.of(BigDecimal.ZERO);
      when(ledgerRepository.createEntry(any(LedgerEntry.class))).thenReturn(UUID.randomUUID());

      ledgerService.recordTransaction(TRANSACTION_ID, amount, DEBIT_ACCOUNT, CREDIT_ACCOUNT, DESCRIPTION);

      verify(ledgerRepository, times(2)).createEntry(any(LedgerEntry.class));
      verify(ledgerRepository).createEntry(argThat(entry ->
        entry.amount().equals(Money.of(BigDecimal.ZERO)) &&
        entry.entryType() == EntryType.DEBIT
      ));
      verify(ledgerRepository).createEntry(argThat(entry ->
        entry.amount().equals(Money.of(BigDecimal.ZERO)) &&
        entry.entryType() == EntryType.CREDIT
      ));
    }

    @Test
    void shouldCreateEntriesWithLargeAmount() {
      Money amount = Money.of(new BigDecimal("999999.99"));
      when(ledgerRepository.createEntry(any(LedgerEntry.class))).thenReturn(UUID.randomUUID());

      ledgerService.recordTransaction(TRANSACTION_ID, amount, DEBIT_ACCOUNT, CREDIT_ACCOUNT, DESCRIPTION);

      verify(ledgerRepository, times(2)).createEntry(argThat(entry ->
        entry.amount().equals(Money.of(new BigDecimal("999999.99")))
      ));
    }
  }

  @Nested
  class GetEntriesForTransaction {

    @Test
    void shouldReturnEntriesForTransaction() {
      LedgerEntry debitEntry = new LedgerEntry(
        UUID.randomUUID(),
        TRANSACTION_ID,
        DEBIT_ACCOUNT,
        AccountType.ASSET,
        EntryType.DEBIT,
        Money.of(new BigDecimal("100.00")),
        DESCRIPTION,
        java.time.Instant.now()
      );
      LedgerEntry creditEntry = new LedgerEntry(
        UUID.randomUUID(),
        TRANSACTION_ID,
        CREDIT_ACCOUNT,
        AccountType.ASSET,
        EntryType.CREDIT,
        Money.of(new BigDecimal("100.00")),
        DESCRIPTION,
        java.time.Instant.now()
      );
      when(ledgerRepository.findByTransactionId(TRANSACTION_ID))
        .thenReturn(List.of(debitEntry, creditEntry));

      List<LedgerEntry> result = ledgerService.getEntriesForTransaction(TRANSACTION_ID);

      assertThat(result).hasSize(2);
      assertThat(result).containsExactly(debitEntry, creditEntry);
      verify(ledgerRepository).findByTransactionId(TRANSACTION_ID);
    }

    @Test
    void shouldReturnEmptyListWhenNoEntriesFound() {
      when(ledgerRepository.findByTransactionId(TRANSACTION_ID)).thenReturn(List.of());

      List<LedgerEntry> result = ledgerService.getEntriesForTransaction(TRANSACTION_ID);

      assertThat(result).isEmpty();
      verify(ledgerRepository).findByTransactionId(TRANSACTION_ID);
    }

    @Test
    void shouldReturnSingleEntry() {
      LedgerEntry singleEntry = new LedgerEntry(
        UUID.randomUUID(),
        TRANSACTION_ID,
        DEBIT_ACCOUNT,
        AccountType.ASSET,
        EntryType.DEBIT,
        Money.of(new BigDecimal("100.00")),
        DESCRIPTION,
        java.time.Instant.now()
      );
      when(ledgerRepository.findByTransactionId(TRANSACTION_ID)).thenReturn(List.of(singleEntry));

      List<LedgerEntry> result = ledgerService.getEntriesForTransaction(TRANSACTION_ID);

      assertThat(result).hasSize(1);
      assertThat(result.get(0)).isEqualTo(singleEntry);
    }

    @Test
    void shouldReturnEntriesForMultipleTransactions() {
      String txn1 = "TXN-001";
      String txn2 = "TXN-002";

      LedgerEntry entry1 = new LedgerEntry(
        UUID.randomUUID(), txn1, DEBIT_ACCOUNT, AccountType.ASSET, EntryType.DEBIT,
        Money.of(new BigDecimal("100.00")), DESCRIPTION, java.time.Instant.now()
      );
      LedgerEntry entry2 = new LedgerEntry(
        UUID.randomUUID(), txn1, CREDIT_ACCOUNT, AccountType.ASSET, EntryType.CREDIT,
        Money.of(new BigDecimal("100.00")), DESCRIPTION, java.time.Instant.now()
      );
      LedgerEntry entry3 = new LedgerEntry(
        UUID.randomUUID(), txn2, DEBIT_ACCOUNT, AccountType.ASSET, EntryType.DEBIT,
        Money.of(new BigDecimal("200.00")), DESCRIPTION, java.time.Instant.now()
      );
      LedgerEntry entry4 = new LedgerEntry(
        UUID.randomUUID(), txn2, CREDIT_ACCOUNT, AccountType.ASSET, EntryType.CREDIT,
        Money.of(new BigDecimal("200.00")), DESCRIPTION, java.time.Instant.now()
      );

      when(ledgerRepository.findByTransactionId(txn1)).thenReturn(List.of(entry1, entry2));
      when(ledgerRepository.findByTransactionId(txn2)).thenReturn(List.of(entry3, entry4));

      List<LedgerEntry> result1 = ledgerService.getEntriesForTransaction(txn1);
      List<LedgerEntry> result2 = ledgerService.getEntriesForTransaction(txn2);

      assertThat(result1).hasSize(2);
      assertThat(result1).containsExactly(entry1, entry2);
      assertThat(result2).hasSize(2);
      assertThat(result2).containsExactly(entry3, entry4);
    }
  }
}
