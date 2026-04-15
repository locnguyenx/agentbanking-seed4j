package com.agentbanking.prepaid.domain.service;

import java.util.regex.Pattern;

public class PhoneValidationService {

  private static final Pattern MALAYSIA_PHONE_PATTERN = Pattern.compile("^\\+?60[1-9]\\d{8,9}$");
  private static final Pattern SHORT_PHONE_PATTERN = Pattern.compile("^0[1-9]\\d{8,9}$");
  private static final Pattern CELCOM_PATTERN = Pattern.compile("^\\+?60[1]\\d{7}$");
  private static final Pattern MAXIS_PATTERN = Pattern.compile("^\\+?60[1][6-9]\\d{7}$");
  private static final Pattern DIGI_PATTERN = Pattern.compile("^\\+?60[1][0-5]\\d{7}$");
  private static final Pattern M1_PATTERN = Pattern.compile("^\\+?60[1][7-9]\\d{7}$");

  public ValidationResult validatePhone(String providerCode, String phoneNumber) {
    String normalized = normalizePhone(phoneNumber);
    if (normalized == null) {
      return ValidationResult.invalid("Invalid phone number format");
    }

    if (providerCode == null) {
      return ValidationResult.valid(normalized);
    }

    return switch (providerCode.toUpperCase()) {
      case "CELCOM" -> validateByProvider(CELCOM_PATTERN, normalized, "Celcom");
      case "MAXIS" -> validateByProvider(MAXIS_PATTERN, normalized, "Maxis");
      case "DIGI" -> validateByProvider(DIGI_PATTERN, normalized, "Digi");
      case "M1" -> validateByProvider(M1_PATTERN, normalized, "M1");
      case "UMOBILE" -> validateByProvider(CELCOM_PATTERN, normalized, "U Mobile");
      default -> ValidationResult.valid(normalized);
    };
  }

  private String normalizePhone(String phone) {
    if (phone == null || phone.isBlank()) {
      return null;
    }
    String normalized = phone.replaceAll("[\\s\\-()]", "");
    if (normalized.startsWith("60") && !normalized.startsWith("+60")) {
      normalized = "+" + normalized;
    } else if (normalized.startsWith("0") && normalized.length() == 10) {
      normalized = "+6" + normalized;
    }
    return normalized;
  }

  private ValidationResult validateByProvider(Pattern pattern, String phone, String providerName) {
    if (pattern.matcher(phone).matches() || MALAYSIA_PHONE_PATTERN.matcher(phone).matches()) {
      return ValidationResult.valid(phone);
    }
    return ValidationResult.invalid("Invalid phone number for " + providerName);
  }

  public static class ValidationResult {
    private final boolean valid;
    private final String normalizedPhone;
    private final String errorMessage;

    private ValidationResult(boolean valid, String normalizedPhone, String errorMessage) {
      this.valid = valid;
      this.normalizedPhone = normalizedPhone;
      this.errorMessage = errorMessage;
    }

    public static ValidationResult valid(String phone) {
      return new ValidationResult(true, phone, null);
    }

    public static ValidationResult invalid(String message) {
      return new ValidationResult(false, null, message);
    }

    public boolean isValid() { return valid; }
    public String getNormalizedPhone() { return normalizedPhone; }
    public String getErrorMessage() { return errorMessage; }
  }
}