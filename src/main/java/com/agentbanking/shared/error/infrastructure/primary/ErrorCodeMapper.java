package com.agentbanking.shared.error.infrastructure.primary;

import java.util.Map;
import java.util.Optional;
import com.agentbanking.shared.error.domain.ErrorCode;

public class ErrorCodeMapper {

  private static final Map<String, ErrorCode> LEGACY_ERROR_MAP = Map.ofEntries(
    Map.entry("INVALID_REQUEST", ErrorCode.ERR_VAL_001),
    Map.entry("INVALID_AMOUNT", ErrorCode.ERR_VAL_002),
    Map.entry("INVALID_ACCOUNT", ErrorCode.ERR_VAL_003),
    Map.entry("INVALID_MYKAD", ErrorCode.ERR_VAL_004),
    Map.entry("INVALID_PHONE", ErrorCode.ERR_VAL_006),
    Map.entry("INVALID_PIN", ErrorCode.ERR_VAL_008),
    Map.entry("FEE_CONFIG_NOT_FOUND", ErrorCode.ERR_VAL_012),
    Map.entry("FEE_CONFIG_EXPIRED", ErrorCode.ERR_VAL_013),

    Map.entry("INSUFFICIENT_FLOAT", ErrorCode.ERR_BIZ_201),
    Map.entry("INSUFFICIENT_BALANCE", ErrorCode.ERR_BIZ_202),
    Map.entry("DAILY_LIMIT_EXCEEDED", ErrorCode.ERR_BIZ_203),
    Map.entry("DAILY_COUNT_LIMIT_EXCEEDED", ErrorCode.ERR_BIZ_204),
    Map.entry("FLOAT_CAP_EXCEEDED", ErrorCode.ERR_BIZ_205),
    Map.entry("AGENT_FLOAT_NOT_FOUND", ErrorCode.ERR_BIZ_206),
    Map.entry("AGENT_DEACTIVATED", ErrorCode.ERR_BIZ_207),
    Map.entry("GEOFENCE_VIOLATION", ErrorCode.ERR_BIZ_209),
    Map.entry("SELF_APPROVAL", ErrorCode.ERR_BIZ_210),
    Map.entry("REVERSAL_WINDOW_EXPIRED", ErrorCode.ERR_BIZ_301),
    Map.entry("ALREADY_REVERSED", ErrorCode.ERR_BIZ_302),

    Map.entry("CBS_TIMEOUT", ErrorCode.ERR_EXT_101),
    Map.entry("CBS_ERROR", ErrorCode.ERR_EXT_102),

    Map.entry("BILLER_TIMEOUT", ErrorCode.ERR_EXT_201),
    Map.entry("BILLER_ERROR", ErrorCode.ERR_EXT_202),

    Map.entry("JPN_UNAVAILABLE", ErrorCode.ERR_EXT_301),
    Map.entry("BIOMETRIC_UNAVAILABLE", ErrorCode.ERR_EXT_302),
    Map.entry("BIOMETRIC_MISMATCH", ErrorCode.ERR_EXT_303),

    Map.entry("PAYNET_TIMEOUT", ErrorCode.ERR_EXT_401),
    Map.entry("PAYNET_ERROR", ErrorCode.ERR_EXT_402),
    Map.entry("ISO_SOCKET_LOST", ErrorCode.ERR_EXT_403),

    Map.entry("INVALID_CREDENTIALS", ErrorCode.ERR_AUTH_001),
    Map.entry("MISSING_TOKEN", ErrorCode.ERR_AUTH_002),
    Map.entry("AUTH_SERVICE_UNAVAILABLE", ErrorCode.ERR_AUTH_003),
    Map.entry("RATE_LIMIT_EXCEEDED", ErrorCode.ERR_AUTH_004),
    Map.entry("ACCOUNT_LOCKED", ErrorCode.ERR_AUTH_004),
    Map.entry("SANCTIONS_BLOCK", ErrorCode.ERR_AUTH_101),
    Map.entry("PIN_LOCKED", ErrorCode.ERR_AUTH_102),
    Map.entry("TOKEN_EXPIRED", ErrorCode.ERR_AUTH_103),

    Map.entry("INTERNAL_ERROR", ErrorCode.ERR_SYS_001),
    Map.entry("GPS_UNAVAILABLE", ErrorCode.ERR_SYS_002),
    Map.entry("TEMPORAL_CORRUPTION", ErrorCode.ERR_SYS_003),
    Map.entry("TEMPORAL_NOT_FOUND", ErrorCode.ERR_SYS_004),

    Map.entry("ISO_ENCODING_FAILED", ErrorCode.ERR_ISO_001),
    Map.entry("ISO_UNMARSHAL_FAILED", ErrorCode.ERR_ISO_002),
    Map.entry("STAN_EXHAUSTED", ErrorCode.ERR_ISO_003),
    Map.entry("SWITCH_CONNECTION_FAILED", ErrorCode.ERR_ISO_002),

    Map.entry("CBS_SOAP_TIMEOUT", ErrorCode.ERR_CBS_001),
    Map.entry("CBS_QUEUE_EMPTY", ErrorCode.ERR_CBS_002),
    Map.entry("CBS_FORMAT_ERROR", ErrorCode.ERR_CBS_003),

    Map.entry("HSM_PIN_BLOCK_FAILED", ErrorCode.ERR_HSM_001),
    Map.entry("HSM_PIN_VERIFY_FAILED", ErrorCode.ERR_HSM_002),
    Map.entry("HSM_KEY_VAULT_VIOLATION", ErrorCode.ERR_HSM_003),
    Map.entry("HSM_CONNECTION_LOST", ErrorCode.ERR_HSM_004),

    Map.entry("BILLER_API_KEY_FAILED", ErrorCode.ERR_BG_001),
    Map.entry("BILLER_IDEMPOTENCY_CONFLICT", ErrorCode.ERR_BG_002)
  );

  public Optional<ErrorCode> fromLegacyError(String legacyError) {
    return Optional.ofNullable(LEGACY_ERROR_MAP.get(legacyError));
  }
}