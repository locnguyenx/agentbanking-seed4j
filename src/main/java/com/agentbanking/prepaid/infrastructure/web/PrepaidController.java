package com.agentbanking.prepaid.infrastructure.web;

import com.agentbanking.prepaid.domain.port.in.TopUpUseCase;
import com.agentbanking.prepaid.domain.port.out.TopUpResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/prepaid")
public class PrepaidController {

    private static final Logger log = LoggerFactory.getLogger(PrepaidController.class);

    private final TopUpUseCase topUpUseCase;

    public PrepaidController(TopUpUseCase topUpUseCase) {
        this.topUpUseCase = topUpUseCase;
    }

    @PostMapping("/topup")
    public ResponseEntity<Map<String, Object>> topUp(@RequestBody TopUpRequest request) {
        log.info("Received top-up request for provider: {}, phone: {}", request.providerCode(), request.phoneNumber());
        
        try {
            TopUpResult result = topUpUseCase.topUp(
                request.providerCode(),
                request.phoneNumber(),
                request.amount(),
                request.idempotencyKey()
            );

            if (result.success()) {
                return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "transactionId", result.transactionId(),
                    "confirmationNumber", result.confirmationNumber(),
                    "amount", request.amount(),
                    "phoneNumber", request.phoneNumber()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "error", Map.of(
                        "code", result.errorCode(),
                        "message", "Top-up failed"
                    )
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of(
                    "code", "ERR_VAL_001",
                    "message", e.getMessage()
                )
            ));
        } catch (Exception e) {
            log.error("Top-up error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "FAILED",
                "error", Map.of(
                    "code", "ERR_EXT_301",
                    "message", "Top-up service unavailable"
                )
            ));
        }
    }

    public record TopUpRequest(
        @NotBlank(message = "Provider code is required") String providerCode,
        @NotBlank(message = "Phone number is required") String phoneNumber,
        @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
        @NotBlank(message = "Idempotency key is required") String idempotencyKey
    ) {}
}