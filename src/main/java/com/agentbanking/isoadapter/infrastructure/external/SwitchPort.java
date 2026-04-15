package com.agentbanking.isoadapter.infrastructure.external;

import com.agentbanking.isoadapter.infrastructure.external.dto.IsoTransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "switch-port",
    url = "${switch.endpoint:http://localhost:9090}"
)
public interface SwitchPort {

    @PostMapping("/iso/0100")
    IsoTransactionResponse sendFinancialTransaction(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody String payload
    );

    @PostMapping("/iso/0420")
    IsoTransactionResponse sendReversal(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody String payload
    );

    @PostMapping("/iso/0800")
    IsoTransactionResponse sendAdminRequest(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody String payload
    );
}