package com.agentbanking.isoadapter.infrastructure.secondary;

import com.agentbanking.isoadapter.domain.model.IsoMessage;
import com.agentbanking.isoadapter.domain.port.in.IsoAdapterService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IsoAdapterServiceImpl implements IsoAdapterService {

  @Override
  public IsoMessage buildWithdrawalMessage(String agentId, String customerAccount, String amount, String stan) {
    return IsoMessage.builder("0200")
      .field(2, customerAccount)
      .field(3, "000000")
      .field(4, amount)
      .field(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")))
      .field(11, stan)
      .field(32, agentId)
      .field(49, "458")
      .build();
  }

  @Override
  public IsoMessage buildDepositMessage(String agentId, String customerAccount, String amount, String stan) {
    return IsoMessage.builder("0200")
      .field(2, customerAccount)
      .field(3, "000000")
      .field(4, amount)
      .field(7, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")))
      .field(11, stan)
      .field(32, agentId)
      .field(49, "458")
      .build();
  }

  @Override
  public IsoMessage buildReversalMessage(String originalStan, String reason) {
    return IsoMessage.builder("0400")
      .field(11, originalStan)
      .field(39, "00")
      .field(90, originalStan)
      .build();
  }

  @Override
  public String parseResponse(IsoMessage response) {
    return response.getField(39);
  }
}
