package com.agentbanking.billeradapter.domain.port.in;

import com.agentbanking.billeradapter.domain.port.out.BillDetails;

public interface GetBillUseCase {
  BillDetails getBillDetails(String billerCode, String ref1, String ref2);
}