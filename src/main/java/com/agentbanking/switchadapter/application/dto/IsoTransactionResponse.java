package com.agentbanking.switchadapter.application.dto;

public record IsoTransactionResponse(
    boolean authorized,
    String responseCode,
    String stan,
    String retrievalReferenceNumber,
    String declineReason
) {
    public static IsoTransactionResponse approved(String stan, String rrn) {
        return new IsoTransactionResponse(true, "00", stan, rrn, null);
    }

    public static IsoTransactionResponse declined(String code, String reason) {
        return new IsoTransactionResponse(false, code, null, null, reason);
    }
}