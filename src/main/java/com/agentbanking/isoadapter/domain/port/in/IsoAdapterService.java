package com.agentbanking.isoadapter.domain.port.in;

import com.agentbanking.isoadapter.domain.model.IsoMessage;

public interface IsoAdapterService {

  IsoMessage buildWithdrawalMessage(String agentId, String customerAccount, String amount, String stan);
  
  IsoMessage buildDepositMessage(String agentId, String customerAccount, String amount, String stan);
  
  IsoMessage buildReversalMessage(String originalStan, String reason);
  
  String parseResponse(IsoMessage response);
}
