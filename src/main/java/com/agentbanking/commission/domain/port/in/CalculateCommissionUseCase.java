package com.agentbanking.commission.domain.port.in;

import com.agentbanking.commission.domain.model.CommissionEntry;
import com.agentbanking.commission.domain.model.CommissionType;
import com.agentbanking.shared.money.domain.Money;

public interface CalculateCommissionUseCase {

  CommissionEntry calculate(String transactionId, String agentId, 
                              String type, Money amount);
}
