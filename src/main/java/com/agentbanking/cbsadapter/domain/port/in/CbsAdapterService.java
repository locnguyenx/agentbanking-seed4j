package com.agentbanking.cbsadapter.domain.port.in;

import com.agentbanking.cbsadapter.domain.model.CbsResponse;

public interface CbsAdapterService {

  CbsResponse checkBalance(String accountId);
  
  CbsResponse debitAccount(String accountId, String amount, String reference);
  
  CbsResponse creditAccount(String accountId, String amount, String reference);
  
  CbsResponse getAccountDetails(String accountId);
}
