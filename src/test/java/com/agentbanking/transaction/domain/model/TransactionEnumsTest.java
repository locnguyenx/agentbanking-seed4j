package com.agentbanking.transaction.domain.model;

import com.agentbanking.UnitTest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

@UnitTest
class TransactionEnumsTest {

  @Test
  void transactionType_hasExpectedValues() {
    assertEquals(6, TransactionType.values().length);
    assertEquals(TransactionType.CASH_WITHDRAWAL, TransactionType.valueOf("CASH_WITHDRAWAL"));
    assertEquals(TransactionType.CASH_DEPOSIT, TransactionType.valueOf("CASH_DEPOSIT"));
    assertEquals(TransactionType.BILL_PAYMENT, TransactionType.valueOf("BILL_PAYMENT"));
    assertEquals(TransactionType.FUND_TRANSFER, TransactionType.valueOf("FUND_TRANSFER"));
    assertEquals(TransactionType.BALANCE_INQUIRY, TransactionType.valueOf("BALANCE_INQUIRY"));
    assertEquals(TransactionType.MINI_STATEMENT, TransactionType.valueOf("MINI_STATEMENT"));
  }

  @Test
  void transactionStatus_hasExpectedValues() {
    assertEquals(5, TransactionStatus.values().length);
    assertEquals(TransactionStatus.PENDING, TransactionStatus.valueOf("PENDING"));
    assertEquals(TransactionStatus.COMPLETED, TransactionStatus.valueOf("COMPLETED"));
    assertEquals(TransactionStatus.FAILED, TransactionStatus.valueOf("FAILED"));
    assertEquals(TransactionStatus.REVERSAL_INITIATED, TransactionStatus.valueOf("REVERSAL_INITIATED"));
    assertEquals(TransactionStatus.REVERSED, TransactionStatus.valueOf("REVERSED"));
  }

  @Test
  void transactionType_notNull() {
    for (TransactionType type : TransactionType.values()) {
      assertNotNull(type);
    }
  }

  @Test
  void transactionStatus_notNull() {
    for (TransactionStatus status : TransactionStatus.values()) {
      assertNotNull(status);
    }
  }
}