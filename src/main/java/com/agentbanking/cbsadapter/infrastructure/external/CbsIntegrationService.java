package com.agentbanking.cbsadapter.infrastructure.external;

import com.agentbanking.cbsadapter.domain.model.CbsResponse;
import com.agentbanking.cbsadapter.infrastructure.external.CbsGatewayPort;
import com.agentbanking.cbsadapter.infrastructure.external.dto.CbsSoapRequest;
import com.agentbanking.cbsadapter.infrastructure.external.dto.CbsSoapResponse;
import java.time.Instant;

public class CbsIntegrationService {

    private final CbsGatewayPort cbsGateway;

    public CbsIntegrationService(CbsGatewayPort cbsGateway) {
        this.cbsGateway = cbsGateway;
    }

    public CbsResponse queryBalance(String traceId, String accountId) {
        try {
            CbsSoapRequest request = new CbsSoapRequest(accountId, "BALANCE_INQ", null, null, traceId);
            CbsSoapResponse response = cbsGateway.checkBalance(traceId, request);
            return mapToCbsResponse(response);
        } catch (Exception e) {
            return CbsResponse.failure(CbsErrorMapper.mapToInternal("CBS_TIMEOUT"), e.getMessage());
        }
    }

    public CbsResponse debitAccount(String traceId, String accountId, String amount, String reference) {
        try {
            CbsSoapRequest request = new CbsSoapRequest(accountId, "DEBIT", amount, reference, traceId);
            CbsSoapResponse response = cbsGateway.debit(traceId, request);
            return mapToCbsResponse(response);
        } catch (Exception e) {
            return CbsResponse.failure(CbsErrorMapper.mapToInternal("CBS_ERROR"), e.getMessage());
        }
    }

    public CbsResponse creditAccount(String traceId, String accountId, String amount, String reference) {
        try {
            CbsSoapRequest request = new CbsSoapRequest(accountId, "CREDIT", amount, reference, traceId);
            CbsSoapResponse response = cbsGateway.credit(traceId, request);
            return mapToCbsResponse(response);
        } catch (Exception e) {
            return CbsResponse.failure(CbsErrorMapper.mapToInternal("CBS_ERROR"), e.getMessage());
        }
    }

    public CbsResponse getAccountDetails(String traceId, String accountId) {
        try {
            CbsSoapRequest request = new CbsSoapRequest(accountId, "ACCT_QUERY", null, null, traceId);
            CbsSoapResponse response = cbsGateway.queryAccount(traceId, request);
            return mapToCbsResponse(response);
        } catch (Exception e) {
            return CbsResponse.failure(CbsErrorMapper.mapToInternal("CBS_ERROR"), e.getMessage());
        }
    }

    private CbsResponse mapToCbsResponse(CbsSoapResponse response) {
        String internalCode = CbsErrorMapper.mapToInternal(response.responseCode());
        boolean success = "CBS_000".equals(internalCode);
        return new CbsResponse(
            success,
            internalCode,
            CbsErrorMapper.getMessage(response.responseCode()),
            response.balance(),
            response.transactionId(),
            Instant.parse(response.timestamp())
        );
    }
}