package com.agentbanking.isoadapter.infrastructure.external.dto;

import com.agentbanking.isoadapter.domain.model.IsoMessage;

public record IsoTransactionRequest(
    String traceId,
    IsoMessage message
) {
    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();
        sb.append(message.mti());
        for (var entry : message.fields().entrySet()) {
            sb.append((char) 0x1C).append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }
}