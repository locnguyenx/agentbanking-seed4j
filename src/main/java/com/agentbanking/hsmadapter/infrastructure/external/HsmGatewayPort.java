package com.agentbanking.hsmadapter.infrastructure.external;

import com.agentbanking.hsmadapter.infrastructure.external.dto.HsmCommandRequest;
import com.agentbanking.hsmadapter.infrastructure.external.dto.HsmCommandResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
    name = "hsm-gateway",
    url = "${hsm.endpoint:http://localhost:8087}",
    configuration = HsmGatewayPortConfiguration.class
)
public interface HsmGatewayPort {

    @PostMapping("/hsm/translate")
    HsmCommandResponse translatePin(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody HsmCommandRequest request
    );

    @PostMapping("/hsm/verify")
    HsmCommandResponse verifyPin(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody HsmCommandRequest request
    );

    @PostMapping("/hsm/generate")
    HsmCommandResponse generatePinBlock(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody HsmCommandRequest request
    );

    @PostMapping("/hsm/extract")
    HsmCommandResponse extractPan(
        @RequestHeader("X-Trace-Id") String traceId,
        @RequestBody HsmCommandRequest request
    );
}