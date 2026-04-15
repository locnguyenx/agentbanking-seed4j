package com.agentbanking.shared.error.domain;

import java.util.Arrays;
import java.util.Optional;

public enum ErrorCode {

  // ERR_VAL_xxx - Validation Errors (14 codes)
  ERR_VAL_001("Invalid request format or missing required field", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_002("Invalid amount format (negative, zero, exceeds precision)", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_003("Invalid account identifier format", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_004("Invalid MyKad format (must be 12 digits)", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_005("Invalid MyKad data format", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_006("Invalid phone number format", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_007("Invalid DuitNow proxy format", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_008("Invalid PIN format or length", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_009("Amount exceeds daily limit for agent tier", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_010("Transaction count exceeds daily limit", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_011("PIN verification failed (3 attempts exceeded)", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_012("Fee configuration not found for transaction type/tier", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_013("Fee configuration expired or not yet effective", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),
  ERR_VAL_014("Fee components mismatch between config and request", ErrorCodeCategory.VALIDATION, 400, "DECLINE"),

  // ERR_BIZ_xxx - Business Errors (30 codes)
  ERR_BIZ_201("Insufficient agent float balance", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_202("Insufficient customer balance", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_203("Daily transaction count limit exceeded", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_204("Daily amount limit exceeded", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_205("Agent float cap exceeded", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_206("Agent float account not found", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_207("Agent account is deactivated", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_208("Insufficient e-wallet balance", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_209("Geofence violation - transaction outside agent location", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_210("Self-approval prohibited (Four-Eyes Principle violation)", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_211("Reason code required for manual adjustment", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_212("Evidence attachment required for discrepancy resolution", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_213("Duplicate account detected", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_214("Invalid biller reference number", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_215("Invalid EPF member number", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_216("Biller service timeout", ErrorCodeCategory.BUSINESS, 400, "RETRY"),
  ERR_BIZ_217("Aggregator/aggregator timeout", ErrorCodeCategory.BUSINESS, 400, "RETRY"),
  ERR_BIZ_218("Smurfing/structuring pattern detected", ErrorCodeCategory.BUSINESS, 400, "REVIEW"),
  ERR_BIZ_219("Duplicate agent registration", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_220("Agent has pending transactions", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_221("Account not found for double-entry posting", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_222("PIN voucher inventory depleted", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_223("eSSP service unavailable", ErrorCodeCategory.BUSINESS, 400, "RETRY"),
  ERR_BIZ_301("Reversal window expired (>5 minutes)", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_302("Transaction already reversed", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),
  ERR_BIZ_303("Settlement paused due to unresolved discrepancies", ErrorCodeCategory.BUSINESS, 400, "REVIEW"),
  ERR_BIZ_304("Amount mismatch in discrepancy resolution", ErrorCodeCategory.BUSINESS, 400, "DECLINE"),

  // ERR_EXT_xxx - External Errors (22 codes)
  ERR_EXT_101("CBS connector timeout", ErrorCodeCategory.EXTERNAL, 504, "RETRY"),
  ERR_EXT_102("CBS connector error", ErrorCodeCategory.EXTERNAL, 502, "RETRY"),
  ERR_EXT_103("CBS settlement file generation failed", ErrorCodeCategory.EXTERNAL, 502, "REVIEW"),
  ERR_EXT_201("Biller gateway timeout", ErrorCodeCategory.EXTERNAL, 504, "RETRY"),
  ERR_EXT_202("Biller gateway error", ErrorCodeCategory.EXTERNAL, 502, "RETRY"),
  ERR_EXT_203("Biller API authentication failure", ErrorCodeCategory.EXTERNAL, 502, "DECLINE"),
  ERR_EXT_204("Biller idempotency conflict", ErrorCodeCategory.EXTERNAL, 409, "DECLINE"),
  ERR_EXT_301("JPN/KYC service unavailable", ErrorCodeCategory.EXTERNAL, 503, "RETRY"),
  ERR_EXT_302("Biometric scanner unavailable", ErrorCodeCategory.EXTERNAL, 503, "RETRY"),
  ERR_EXT_303("Biometric verification mismatch", ErrorCodeCategory.EXTERNAL, 400, "DECLINE"),
  ERR_EXT_401("PayNet ISO timeout", ErrorCodeCategory.EXTERNAL, 504, "RETRY"),
  ERR_EXT_402("PayNet ISO error", ErrorCodeCategory.EXTERNAL, 502, "RETRY"),
  ERR_EXT_403("ISO TCP socket connection lost", ErrorCodeCategory.EXTERNAL, 502, "RETRY"),
  ERR_EXT_404("STAN exhaustion (999999 reached)", ErrorCodeCategory.EXTERNAL, 500, "REVIEW"),

  // ERR_AUTH_xxx - Authentication Errors (10 codes)
  ERR_AUTH_001("Invalid OAuth2 token", ErrorCodeCategory.AUTHENTICATION, 401, "DECLINE"),
  ERR_AUTH_002("Missing authentication token", ErrorCodeCategory.AUTHENTICATION, 401, "DECLINE"),
  ERR_AUTH_003("Service unavailable for authentication", ErrorCodeCategory.AUTHENTICATION, 503, "RETRY"),
  ERR_AUTH_004("Rate limit exceeded", ErrorCodeCategory.AUTHENTICATION, 429, "DECLINE"),
  ERR_AUTH_101("Account/terminal sanctioned or blocked", ErrorCodeCategory.AUTHENTICATION, 403, "REVIEW"),
  ERR_AUTH_102("PIN locked after max attempts", ErrorCodeCategory.AUTHENTICATION, 403, "DECLINE"),
  ERR_AUTH_103("Token expired", ErrorCodeCategory.AUTHENTICATION, 401, "DECLINE"),

  // ERR_SYS_xxx - System Errors (8 codes)
  ERR_SYS_001("Internal server error", ErrorCodeCategory.SYSTEM, 500, "REVIEW"),
  ERR_SYS_002("GPS service unavailable", ErrorCodeCategory.SYSTEM, 503, "RETRY"),
  ERR_SYS_003("Temporal workflow state corruption", ErrorCodeCategory.SYSTEM, 500, "REVIEW"),
  ERR_SYS_004("Temporal workflow not found", ErrorCodeCategory.SYSTEM, 404, "REVIEW"),
  ERR_SYS_005("Temporal activity timeout misconfiguration", ErrorCodeCategory.SYSTEM, 500, "REVIEW"),

  // ERR_ISO_xxx - ISO Translation Errors (4 codes)
  ERR_ISO_001("ISO 8583 bitmap generation failed", ErrorCodeCategory.ISO_TRANSLATION, 500, "REVIEW"),
  ERR_ISO_002("ISO response unmarshal failed", ErrorCodeCategory.ISO_TRANSLATION, 500, "REVIEW"),
  ERR_ISO_003("STAN generation failed", ErrorCodeCategory.ISO_TRANSLATION, 500, "REVIEW"),
  ERR_ISO_004("PayNet heartbeat timeout", ErrorCodeCategory.ISO_TRANSLATION, 504, "RETRY"),

  // ERR_CBS_xxx - CBS Connector Errors (3 codes)
  ERR_CBS_001("SOAP/MQ request timeout", ErrorCodeCategory.CBS_CONNECTOR, 504, "RETRY"),
  ERR_CBS_002("CBS reply queue empty", ErrorCodeCategory.CBS_CONNECTOR, 504, "RETRY"),
  ERR_CBS_003("CBS flat-file format error", ErrorCodeCategory.CBS_CONNECTOR, 500, "REVIEW"),

  // ERR_HSM_xxx - HSM Wrapper Errors (4 codes)
  ERR_HSM_001("HSM PIN translation failed", ErrorCodeCategory.HSM_WRAPPER, 500, "RETRY"),
  ERR_HSM_002("HSM PIN verification failed", ErrorCodeCategory.HSM_WRAPPER, 500, "REVIEW"),
  ERR_HSM_003("HSM key vault access violation", ErrorCodeCategory.HSM_WRAPPER, 500, "STOP_ALERT"),
  ERR_HSM_004("HSM connection lost", ErrorCodeCategory.HSM_WRAPPER, 503, "RETRY"),

  // ERR_BG_xxx - Biller Gateway Errors (2 codes)
  ERR_BG_001("Biller API key injection failed", ErrorCodeCategory.BILLER_GATEWAY, 500, "REVIEW"),
  ERR_BG_002("Biller idempotency key conflict", ErrorCodeCategory.BILLER_GATEWAY, 409, "DECLINE"),

  // ERR_EM_xxx - Error Mapping Errors (3 codes)
  ERR_EM_001("Unknown legacy error code received", ErrorCodeCategory.ERROR_MAPPING, 500, "STOP_ALERT"),
  ERR_EM_002("Fallback mapping applied for unknown error", ErrorCodeCategory.ERROR_MAPPING, 500, "REVIEW"),
  ERR_EM_003("Error normalization pipeline failure", ErrorCodeCategory.ERROR_MAPPING, 500, "REVIEW");

  private final String message;
  private final ErrorCodeCategory category;
  private final int httpStatus;
  private final String actionCode;

  ErrorCode(String message, ErrorCodeCategory category, int httpStatus, String actionCode) {
    this.message = message;
    this.category = category;
    this.httpStatus = httpStatus;
    this.actionCode = actionCode;
  }

  public String message() {
    return message;
  }

  public ErrorCodeCategory category() {
    return category;
  }

  public int httpStatus() {
    return httpStatus;
  }

  public String actionCode() {
    return actionCode;
  }

  public static Optional<ErrorCode> fromCode(String code) {
    return Arrays.stream(values())
      .filter(c -> c.name().equals(code))
      .findFirst();
  }
}