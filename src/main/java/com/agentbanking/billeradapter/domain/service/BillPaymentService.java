package com.agentbanking.billeradapter.domain.service;

import com.agentbanking.billeradapter.domain.model.Biller;
import com.agentbanking.billeradapter.domain.port.in.GetBillUseCase;
import com.agentbanking.billeradapter.domain.port.in.PayBillUseCase;
import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.BillerGatewayPort;
import com.agentbanking.billeradapter.domain.port.out.BillerRegistryPort;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import com.agentbanking.billeradapter.domain.port.out.ReversalResult;

import java.math.BigDecimal;
import java.util.List;

public class BillPaymentService implements GetBillUseCase, PayBillUseCase {

  private final BillerGatewayPort billerGatewayPort;
  private final BillerRegistryPort billerRegistryPort;

  public BillPaymentService(BillerGatewayPort billerGatewayPort, BillerRegistryPort billerRegistryPort) {
    this.billerGatewayPort = billerGatewayPort;
    this.billerRegistryPort = billerRegistryPort;
  }

  @Override
  public BillDetails getBillDetails(String billerCode, String ref1, String ref2) {
    Biller biller = billerRegistryPort.findByCode(billerCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown biller code: " + billerCode));

    if (!biller.supportsInquiry()) {
      throw new IllegalArgumentException("Biller does not support inquiry: " + billerCode);
    }

    return billerGatewayPort.getBillDetails(billerCode, ref1, ref2);
  }

  @Override
  public PaymentResult payBill(String billerCode, String ref1, String ref2, BigDecimal amount, String idempotencyKey) {
    Biller biller = billerRegistryPort.findByCode(billerCode)
        .orElseThrow(() -> new IllegalArgumentException("Unknown biller code: " + billerCode));

    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }

    return billerGatewayPort.payBill(billerCode, ref1, ref2, amount);
  }

  public ReversalResult reverse(String transactionId, String reason) {
    return billerGatewayPort.reverseBill(transactionId, reason);
  }

  public List<Biller> getSupportedBillers() {
    return billerRegistryPort.findAll();
  }
}