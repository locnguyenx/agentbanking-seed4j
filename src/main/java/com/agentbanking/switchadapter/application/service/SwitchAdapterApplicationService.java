package com.agentbanking.switchadapter.application.service;

import com.agentbanking.switchadapter.application.dto.InternalTransactionRequest;
import com.agentbanking.switchadapter.application.dto.IsoTransactionResponse;
import com.agentbanking.switchadapter.domain.model.IsoMessage;
import com.agentbanking.switchadapter.domain.port.out.SwitchPort;
import com.agentbanking.switchadapter.domain.service.IsoTranslationService;

public class SwitchAdapterApplicationService {

    private final IsoTranslationService translationService;
    private final SwitchPort switchPort;

    public SwitchAdapterApplicationService(
            IsoTranslationService translationService,
            SwitchPort switchPort) {
        this.translationService = translationService;
        this.switchPort = switchPort;
    }

    public IsoTransactionResponse processTransaction(InternalTransactionRequest request) {
        IsoMessage isoMessage = translationService.translate(request);
        IsoMessage response = switchPort.send(isoMessage);
        return translationService.translateFromIso(response);
    }

    public IsoTransactionResponse processReversal(InternalTransactionRequest request) {
        IsoMessage isoMessage = translationService.translate(request);
        IsoMessage response = switchPort.sendReversal(isoMessage);
        return translationService.translateFromIso(response);
    }
}