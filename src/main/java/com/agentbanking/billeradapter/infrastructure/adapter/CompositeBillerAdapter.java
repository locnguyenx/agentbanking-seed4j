package com.agentbanking.billeradapter.infrastructure.adapter;

import com.agentbanking.billeradapter.domain.port.out.BillDetails;
import com.agentbanking.billeradapter.domain.port.out.BillerGatewayPort;
import com.agentbanking.billeradapter.domain.port.out.PaymentResult;
import com.agentbanking.billeradapter.domain.port.out.ReversalResult;
import com.agentbanking.billeradapter.infrastructure.external.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

public class CompositeBillerAdapter implements BillerGatewayPort {

  private static final Logger log = LoggerFactory.getLogger(CompositeBillerAdapter.class);

  private final TnbBillerClient tnbClient;
  private final MaxisBillerClient maxisClient;
  private final JompayBillerClient jompayClient;
  private final AstroBillerClient astroClient;
  private final EpfBillerClient epfClient;
  private final TmBillerClient tmClient;

  public CompositeBillerAdapter(
      TnbBillerClient tnbClient,
      MaxisBillerClient maxisClient,
      JompayBillerClient jompayClient,
      AstroBillerClient astroClient,
      EpfBillerClient epfClient,
      TmBillerClient tmClient) {
    this.tnbClient = tnbClient;
    this.maxisClient = maxisClient;
    this.jompayClient = jompayClient;
    this.astroClient = astroClient;
    this.epfClient = epfClient;
    this.tmClient = tmClient;
  }

  @Override
  public BillDetails getBillDetails(String billerCode, String ref1, String ref2) {
    return switch (billerCode.toUpperCase()) {
      case "TNB" -> tnbClient.getBillDetails(ref1, ref2);
      case "MAXIS", "DIGI", "UNIFI" -> maxisClient.getBillDetails(ref1, ref2);
      case "JOMPAY" -> jompayClient.getBillDetails(ref1, ref2);
      case "ASTRO" -> astroClient.getBillDetails(ref1, ref2);
      case "EPF" -> epfClient.getBillDetails(ref1, ref2);
      case "TM" -> tmClient.getBillDetails(ref1, ref2);
      default -> throw new IllegalArgumentException("Unknown biller: " + billerCode);
    };
  }

  @Override
  public PaymentResult payBill(String billerCode, String ref1, String ref2, BigDecimal amount) {
    return switch (billerCode.toUpperCase()) {
      case "TNB" -> tnbClient.payBill(ref1, ref2, amount);
      case "MAXIS", "DIGI", "UNIFI" -> maxisClient.payBill(ref1, ref2, amount);
      case "JOMPAY" -> jompayClient.payBill(ref1, ref2, amount);
      case "ASTRO" -> astroClient.payBill(ref1, ref2, amount);
      case "EPF" -> epfClient.payBill(ref1, ref2, amount);
      case "TM" -> tmClient.payBill(ref1, ref2, amount);
      default -> throw new IllegalArgumentException("Unknown biller: " + billerCode);
    };
  }

  @Override
  public ReversalResult reverseBill(String transactionId, String reason) {
    log.info("Routing reversal for transaction: {}", transactionId);
    if (transactionId.startsWith("TXN-TNB")) {
      return tnbClient.reverseBill(transactionId, reason);
    } else if (transactionId.startsWith("TXN-MAXIS") || transactionId.startsWith("TXN-DIGI") || transactionId.startsWith("TXN-UNIFI")) {
      return maxisClient.reverseBill(transactionId, reason);
    } else if (transactionId.startsWith("TXN-ASTRO")) {
      return astroClient.reverseBill(transactionId, reason);
    } else if (transactionId.startsWith("TXN-TM")) {
      return tmClient.reverseBill(transactionId, reason);
    }
    return new ReversalResult(false, null, "Unable to reverse transaction");
  }
}