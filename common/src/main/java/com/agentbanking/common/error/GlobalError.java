package com.agentbanking.common.error;

import java.time.OffsetDateTime;

public record GlobalError(
    String status,
    ErrorDetail error,
    String traceId,
    OffsetDateTime timestamp
) {
    public record ErrorDetail(
        String code,
        String message,
        String actionCode,
        String traceId,
        OffsetDateTime timestamp
    ) {}

    public static GlobalError of(String code, String message, String actionCode, String traceId) {
        var now = OffsetDateTime.now();
        return new GlobalError(
            "FAILED",
            new ErrorDetail(code, message, actionCode, traceId, now),
            traceId,
            now
        );
    }
}
