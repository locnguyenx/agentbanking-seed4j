package com.agentbanking.cbsadapter.infrastructure.secondary;

import com.agentbanking.cbsadapter.domain.model.CbsResponse;
import com.agentbanking.cbsadapter.domain.port.in.CbsAdapterService;
import java.time.Instant;

public class CbsAdapterServiceImpl implements CbsAdapterService {

  @Override
  public CbsResponse checkBalance(String accountId) {
    // In production, would call CBS SOAP/MQ service
    return new CbsResponse(
      true,
      "00",
      "Success",
      "10000.00",
      null,
      Instant.now()
    );
  }

  @Override
  public CbsResponse debitAccount(String accountId, String amount, String reference) {
    // In production, would call CBS SOAP/MQ service
    return new CbsResponse(
      true,
      "00",
      "Debit successful",
      null,
      "TXN-" + System.currentTimeMillis(),
      Instant.now()
    );
  }

  @Override
  public CbsResponse creditAccount(String accountId, String amount, String reference) {
    // In production, would call CBS SOAP/MQ service
    return new CbsResponse(
      true,
      "00",
      "Credit successful",
      null,
      "TXN-" + System.currentTimeMillis(),
      Instant.now()
    );
  }

  @Override
  public CbsResponse getAccountDetails(String accountId) {
    // In production, would call CBS SOAP/MQ service
    return new CbsResponse(
      true,
      "00",
      "Account details retrieved",
      "10000.00",
      null,
      Instant.now()
    );
  }
}
