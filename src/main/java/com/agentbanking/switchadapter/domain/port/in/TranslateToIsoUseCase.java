package com.agentbanking.switchadapter.domain.port.in;

import com.agentbanking.switchadapter.domain.model.IsoMessage;
import com.agentbanking.switchadapter.application.dto.InternalTransactionRequest;

public interface TranslateToIsoUseCase {
    IsoMessage translate(InternalTransactionRequest request);
}