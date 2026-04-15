package com.agentbanking.isoadapter.infrastructure.external;

import java.util.Map;

public class IsoErrorMapper {

    private static final Map<String, String> ISO_TO_INTERNAL = Map.ofEntries(
        Map.entry("00", "ERR_BIZ_000"),
        Map.entry("01", "ERR_ISO_001"),
        Map.entry("02", "ERR_ISO_002"),
        Map.entry("03", "ERR_ISO_003"),
        Map.entry("04", "ERR_ISO_004"),
        Map.entry("05", "ERR_ISO_005"),
        Map.entry("06", "ERR_ISO_006"),
        Map.entry("10", "ERR_ISO_010"),
        Map.entry("11", "ERR_ISO_011"),
        Map.entry("12", "ERR_ISO_012"),
        Map.entry("13", "ERR_ISO_013"),
        Map.entry("14", "ERR_ISO_014"),
        Map.entry("15", "ERR_ISO_015"),
        Map.entry("20", "ERR_ISO_020"),
        Map.entry("21", "ERR_ISO_021"),
        Map.entry("30", "ERR_ISO_030"),
        Map.entry("31", "ERR_ISO_031"),
        Map.entry("39", "ERR_ISO_039"),
        Map.entry("51", "ERR_ISO_051"),
        Map.entry("52", "ERR_ISO_052"),
        Map.entry("53", "ERR_ISO_053"),
        Map.entry("54", "ERR_ISO_054"),
        Map.entry("55", "ERR_ISO_055"),
        Map.entry("56", "ERR_ISO_056"),
        Map.entry("57", "ERR_ISO_057"),
        Map.entry("58", "ERR_ISO_058"),
        Map.entry("59", "ERR_ISO_059"),
        Map.entry("60", "ERR_ISO_060"),
        Map.entry("61", "ERR_ISO_061"),
        Map.entry("62", "ERR_ISO_062"),
        Map.entry("63", "ERR_ISO_063"),
        Map.entry("64", "ERR_ISO_064"),
        Map.entry("65", "ERR_ISO_065"),
        Map.entry("75", "ERR_ISO_075"),
        Map.entry("76", "ERR_ISO_076"),
        Map.entry("77", "ERR_ISO_077"),
        Map.entry("78", "ERR_ISO_078"),
        Map.entry("91", "ERR_ISO_091"),
        Map.entry("92", "ERR_ISO_092"),
        Map.entry("93", "ERR_ISO_093"),
        Map.entry("95", "ERR_ISO_095"),
        Map.entry("96", "ERR_ISO_096"),
        Map.entry("97", "ERR_ISO_097"),
        Map.entry("98", "ERR_ISO_098"),
        Map.entry("99", "ERR_ISO_099")
    );

    private static final Map<String, String> ISO_MESSAGES = Map.ofEntries(
        Map.entry("00", "Approved"),
        Map.entry("01", "Refer to card issuer"),
        Map.entry("02", "Refer to card issuer"),
        Map.entry("03", "Invalid merchant"),
        Map.entry("04", "Pickup card"),
        Map.entry("05", "Do not honor"),
        Map.entry("06", "Error"),
        Map.entry("10", "Bad currency"),
        Map.entry("11", "Bad amount"),
        Map.entry("12", "Bad date"),
        Map.entry("13", "Bad captured"),
        Map.entry("14", "Bad account"),
        Map.entry("15", "Unknown account"),
        Map.entry("20", "Bad respone"),
        Map.entry("21", "Card expired"),
        Map.entry("30", "Bad message format"),
        Map.entry("31", "Expired card"),
        Map.entry("39", "Unknown error"),
        Map.entry("51", "Insufficient funds"),
        Map.entry("52", "No checking account"),
        Map.entry("53", "No savings account"),
        Map.entry("54", "Expired card"),
        Map.entry("55", "Incorrect PIN"),
        Map.entry("56", "Unknown account"),
        Map.entry("57", "Card limit exceeded"),
        Map.entry("58", "Transaction not permitted"),
        Map.entry("59", "Suspected fraud"),
        Map.entry("60", "Bad transfer"),
        Map.entry("61", "Daily limit exceeded"),
        Map.entry("62", "Restricted card"),
        Map.entry("63", "Security violation"),
        Map.entry("64", "Bad original"),
        Map.entry("65", "Daily count limit"),
        Map.entry("75", "PIN tries exceeded"),
        Map.entry("76", "Bad stan"),
        Map.entry("77", "Bad date match"),
        Map.entry("78", "No account"),
        Map.entry("91", "Issuer unavailable"),
        Map.entry("92", "Route not found"),
        Map.entry("93", "Invalid reversal"),
        Map.entry("95", "Reconcile error"),
        Map.entry("96", "System error"),
        Map.entry("97", "Duplicate txn"),
        Map.entry("98", "Timeout"),
        Map.entry("99", "Network error")
    );

    public static String mapToInternal(String isoCode) {
        return ISO_TO_INTERNAL.getOrDefault(isoCode, "ERR_ISO_999");
    }

    public static String getMessage(String isoCode) {
        return ISO_MESSAGES.getOrDefault(isoCode, "Unknown error");
    }

    public static String getActionCode(String isoCode) {
        if (isoCode == null || isoCode.isEmpty()) {
            return "RETRY";
        }
        return switch (isoCode) {
            case "04", "75" -> "REVIEW";
            case "01", "02", "03", "05", "06", "10", "11", "12", "13", "14", "15", "20", "21", "30", "31", "39", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "64", "65", "76", "77", "78", "95", "96", "97", "98", "99" -> "DECLINE";
            case "61", "62", "63", "91", "92", "93" -> "RETRY";
            default -> "DECLINE";
        };
    }
}