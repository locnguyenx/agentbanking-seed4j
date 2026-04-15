package com.agentbanking.switchadapter.domain.service;

import com.agentbanking.switchadapter.application.dto.InternalTransactionRequest;
import com.agentbanking.switchadapter.application.dto.IsoTransactionResponse;
import com.agentbanking.switchadapter.domain.model.IsoMessage;
import com.agentbanking.switchadapter.domain.port.in.TranslateToIsoUseCase;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class IsoTranslationService implements TranslateToIsoUseCase {

    private static final AtomicInteger stanCounter = new AtomicInteger(1);

    @Override
    public IsoMessage translate(InternalTransactionRequest request) {
        return translateToIso(request);
    }

    public IsoMessage translateToIso(InternalTransactionRequest request) {
        String mti = getMtiForTransaction(request.transactionType());
        Map<Integer, String> fields = new HashMap<>();

        fields.put(2, request.pan());
        fields.put(3, getProcessingCode(request.transactionType()));
        fields.put(4, formatAmount(request.amount()));
        fields.put(7, formatTransmissionDateTime());
        fields.put(11, generateStan());
        fields.put(12, formatLocalTime());
        fields.put(13, formatLocalDate());
        fields.put(32, request.acquiringInstitutionId() != null ? request.acquiringInstitutionId() : "ACQ001");
        fields.put(37, request.retrievalReferenceNumber() != null ? request.retrievalReferenceNumber() : generateRrn());
        fields.put(41, request.terminalId());
        fields.put(42, request.merchantId());

        return new IsoMessage(mti, fields, null);
    }

    public IsoTransactionResponse translateFromIso(IsoMessage message) {
        String responseCode = message.fields().get(39);
        String stan = message.fields().get(11);
        String rrn = message.fields().get(37);

        boolean authorized = "00".equals(responseCode);
        String declineReason = getDeclineReason(responseCode);

        return new IsoTransactionResponse(authorized, responseCode, stan, rrn, declineReason);
    }

    private String getMtiForTransaction(String transactionType) {
        return switch (transactionType) {
            case "CASH_WITHDRAWAL", "CASH_DEPOSIT", "PAYMENT" -> "0100";
            case "REVERSAL" -> "0400";
            default -> "0100";
        };
    }

    private String getProcessingCode(String transactionType) {
        return switch (transactionType) {
            case "CASH_WITHDRAWAL" -> "00";
            case "CASH_DEPOSIT" -> "01";
            case "PAYMENT" -> "00";
            default -> "00";
        };
    }

    private String formatAmount(BigDecimal amount) {
        long amountInCents = amount.multiply(new BigDecimal("100")).longValue();
        return String.format("%012d", amountInCents);
    }

    private String formatTransmissionDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));
    }

    private String formatLocalTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
    }

    private String formatLocalDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
    }

    private String generateStan() {
        int stan = stanCounter.getAndIncrement();
        if (stan > 999999) {
            stanCounter.set(1);
            stan = 1;
        }
        return String.format("%06d", stan);
    }

    private String generateRrn() {
        return String.format("%012d", System.currentTimeMillis() % 1000000000000L);
    }

    private String getDeclineReason(String responseCode) {
        if (responseCode == null) return "Unknown error";
        return switch (responseCode) {
            case "00" -> null;
            case "05" -> "Do Not Honor";
            case "13" -> "Invalid Amount";
            case "51" -> "Insufficient Funds";
            case "54" -> "Expired Card";
            case "91" -> "Issuer Unavailable";
            default -> "Unknown error";
        };
    }
}