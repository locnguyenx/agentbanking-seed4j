package com.agentbanking.cbsadapter.infrastructure.external;

import com.agentbanking.cbsadapter.infrastructure.external.dto.CbsSoapRequest;
import com.agentbanking.cbsadapter.infrastructure.external.dto.CbsSoapResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "cbs-gateway",
    url = "${cbs.endpoint:http://localhost:8086}",
    configuration = CbsGatewayPortConfiguration.class
)
public interface CbsGatewayPort {

    @PostMapping("/cbs/balance")
    CbsSoapResponse checkBalance(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody CbsSoapRequest request
    );

    @PostMapping("/cbs/debit")
    CbsSoapResponse debit(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody CbsSoapRequest request
    );

    @PostMapping("/cbs/credit")
    CbsSoapResponse credit(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody CbsSoapRequest request
    );

    @PostMapping("/cbs/account/query")
    CbsSoapResponse queryAccount(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody CbsSoapRequest request
    );
}