package com.agentbanking.hsmadapter.infrastructure.external;

import java.util.Map;

public class HsmErrorMapper {

    private static final Map<String, String> HSM_TO_INTERNAL = Map.ofEntries(
        Map.entry("HSM000", "ERR_BIZ_000"),
        Map.entry("HSM001", "ERR_HSM_001"),
        Map.entry("HSM002", "ERR_HSM_002"),
        Map.entry("HSM003", "ERR_HSM_003"),
        Map.entry("HSM004", "ERR_HSM_004"),
        Map.entry("HSM005", "ERR_HSM_005"),
        Map.entry("HSM006", "ERR_HSM_006"),
        Map.entry("HSM007", "ERR_HSM_007"),
        Map.entry("HSM008", "ERR_HSM_008"),
        Map.entry("HSM009", "ERR_HSM_009"),
        Map.entry("HSM010", "ERR_VAL_008"),
        Map.entry("HSM011", "ERR_VAL_009")
    );

    private static final Map<String, String> HSM_MESSAGES = Map.ofEntries(
        Map.entry("HSM000", "Success"),
        Map.entry("HSM001", "HSM connection error"),
        Map.entry("HSM002", "HSM timeout"),
        Map.entry("HSM003", "Invalid PIN block format"),
        Map.entry("HSM004", "Invalid account number"),
        Map.entry("HSM005", "HSM internal error"),
        Map.entry("HSM006", "Key not found"),
        Map.entry("HSM007", "Key expired"),
        Map.entry("HSM008", "HSM unavailable"),
        Map.entry("HSM009", "Invalid command"),
        Map.entry("HSM010", "Invalid PIN"),
        Map.entry("HSM011", "PIN blocked")
    );

    public static String mapToInternal(String hsmCode) {
        if (hsmCode == null || hsmCode.isEmpty()) {
            return "ERR_HSM_009";
        }
        return HSM_TO_INTERNAL.getOrDefault(hsmCode.toUpperCase(), "ERR_HSM_009");
    }

    public static String getMessage(String hsmCode) {
        if (hsmCode == null || hsmCode.isEmpty()) {
            return "Unknown HSM error";
        }
        return HSM_MESSAGES.getOrDefault(hsmCode.toUpperCase(), "Unknown HSM error");
    }

    public static String getActionCode(String hsmCode) {
        if (hsmCode == null || hsmCode.isEmpty()) {
            return "RETRY";
        }
        return switch (hsmCode.toUpperCase()) {
            case "HSM001", "HSM002", "HSM005", "HSM008" -> "RETRY";
            case "HSM003", "HSM004", "HSM006", "HSM007", "HSM009", "HSM010", "HSM011" -> "DECLINE";
            default -> "RETRY";
        };
    }
}