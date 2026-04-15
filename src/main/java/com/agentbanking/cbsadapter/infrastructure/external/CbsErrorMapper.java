package com.agentbanking.cbsadapter.infrastructure.external;

import java.util.Map;

public class CbsErrorMapper {

    private static final Map<String, String> CBS_TO_INTERNAL = Map.ofEntries(
        Map.entry("CBS000", "ERR_BIZ_000"),
        Map.entry("CBS001", "ERR_EXT_101"),
        Map.entry("CBS002", "ERR_EXT_102"),
        Map.entry("CBS003", "ERR_BIZ_202"),
        Map.entry("CBS004", "ERR_BIZ_206"),
        Map.entry("CBS005", "ERR_BIZ_207"),
        Map.entry("CBS006", "ERR_VAL_003"),
        Map.entry("CBS007", "ERR_VAL_012"),
        Map.entry("CBS008", "ERR_VAL_015"),
        Map.entry("CBS009", "ERR_EXT_103"),
        Map.entry("CBS010", "ERR_EXT_104"),
        Map.entry("CBS011", "ERR_BIZ_208"),
        Map.entry("CBS012", "ERR_BIZ_301"),
        Map.entry("CBS013", "ERR_BIZ_302")
    );

    private static final Map<String, String> CBS_MESSAGES = Map.ofEntries(
        Map.entry("CBS000", "Success"),
        Map.entry("CBS001", "CBS timeout"),
        Map.entry("CBS002", "CBS connection error"),
        Map.entry("CBS003", "Insufficient balance"),
        Map.entry("CBS004", "Account not found"),
        Map.entry("CBS005", "Account closed"),
        Map.entry("CBS006", "Invalid account format"),
        Map.entry("CBS007", "Fee config not found"),
        Map.entry("CBS008", "Invalid transaction type"),
        Map.entry("CBS009", "CBS system error"),
        Map.entry("CBS010", "CBS unavailable"),
        Map.entry("CBS011", "Account restricted"),
        Map.entry("CBS012", "Reversal window expired"),
        Map.entry("CBS013", "Transaction already reversed")
    );

    public static String mapToInternal(String cbsCode) {
        if (cbsCode == null || cbsCode.isEmpty()) {
            return "ERR_EXT_102";
        }
        return CBS_TO_INTERNAL.getOrDefault(cbsCode.toUpperCase(), "ERR_EXT_102");
    }

    public static String getMessage(String cbsCode) {
        if (cbsCode == null || cbsCode.isEmpty()) {
            return "Unknown CBS error";
        }
        return CBS_MESSAGES.getOrDefault(cbsCode.toUpperCase(), "Unknown CBS error");
    }

    public static String getActionCode(String cbsCode) {
        if (cbsCode == null || cbsCode.isEmpty()) {
            return "RETRY";
        }
        return switch (cbsCode.toUpperCase()) {
            case "CBS001", "CBS009", "CBS010" -> "RETRY";
            case "CBS003", "CBS004", "CBS005", "CBS006", "CBS007", "CBS008", "CBS011", "CBS012", "CBS013" -> "DECLINE";
            default -> "RETRY";
        };
    }
}