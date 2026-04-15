package com.agentbanking.shared.error.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.agentbanking.UnitTest;
import com.agentbanking.shared.error.domain.ErrorCode;

@UnitTest
@DisplayName("ErrorCodeMapper")
class ErrorCodeMapperTest {

  private final ErrorCodeMapper mapper = new ErrorCodeMapper();

  @Nested
  class LegacyErrorMapping {

    @Test
    void shouldMapInvalidRequestToVal001() {
      assertThat(mapper.fromLegacyError("INVALID_REQUEST")).contains(ErrorCode.ERR_VAL_001);
    }

    @Test
    void shouldMapInvalidAmountToVal002() {
      assertThat(mapper.fromLegacyError("INVALID_AMOUNT")).contains(ErrorCode.ERR_VAL_002);
    }

    @Test
    void shouldMapInvalidAccountToVal003() {
      assertThat(mapper.fromLegacyError("INVALID_ACCOUNT")).contains(ErrorCode.ERR_VAL_003);
    }

    @Test
    void shouldMapInvalidMyKadToVal004() {
      assertThat(mapper.fromLegacyError("INVALID_MYKAD")).contains(ErrorCode.ERR_VAL_004);
    }

    @Test
    void shouldMapInvalidPhoneToVal006() {
      assertThat(mapper.fromLegacyError("INVALID_PHONE")).contains(ErrorCode.ERR_VAL_006);
    }

    @Test
    void shouldMapInvalidPinToVal008() {
      assertThat(mapper.fromLegacyError("INVALID_PIN")).contains(ErrorCode.ERR_VAL_008);
    }

    @Test
    void shouldMapFeeConfigNotFoundToVal012() {
      assertThat(mapper.fromLegacyError("FEE_CONFIG_NOT_FOUND")).contains(ErrorCode.ERR_VAL_012);
    }

    @Test
    void shouldMapFeeConfigExpiredToVal013() {
      assertThat(mapper.fromLegacyError("FEE_CONFIG_EXPIRED")).contains(ErrorCode.ERR_VAL_013);
    }

    @Test
    void shouldMapInsufficientFloatToBiz201() {
      assertThat(mapper.fromLegacyError("INSUFFICIENT_FLOAT")).contains(ErrorCode.ERR_BIZ_201);
    }

    @Test
    void shouldMapInsufficientBalanceToBiz202() {
      assertThat(mapper.fromLegacyError("INSUFFICIENT_BALANCE")).contains(ErrorCode.ERR_BIZ_202);
    }

    @Test
    void shouldMapDailyLimitExceededToBiz203() {
      assertThat(mapper.fromLegacyError("DAILY_LIMIT_EXCEEDED")).contains(ErrorCode.ERR_BIZ_203);
    }

    @Test
    void shouldMapDailyCountLimitExceededToBiz204() {
      assertThat(mapper.fromLegacyError("DAILY_COUNT_LIMIT_EXCEEDED")).contains(ErrorCode.ERR_BIZ_204);
    }

    @Test
    void shouldMapFloatCapExceededToBiz205() {
      assertThat(mapper.fromLegacyError("FLOAT_CAP_EXCEEDED")).contains(ErrorCode.ERR_BIZ_205);
    }

    @Test
    void shouldMapAgentFloatNotFoundToBiz206() {
      assertThat(mapper.fromLegacyError("AGENT_FLOAT_NOT_FOUND")).contains(ErrorCode.ERR_BIZ_206);
    }

    @Test
    void shouldMapAgentDeactivatedToBiz207() {
      assertThat(mapper.fromLegacyError("AGENT_DEACTIVATED")).contains(ErrorCode.ERR_BIZ_207);
    }

    @Test
    void shouldMapGeofenceViolationToBiz209() {
      assertThat(mapper.fromLegacyError("GEOFENCE_VIOLATION")).contains(ErrorCode.ERR_BIZ_209);
    }

    @Test
    void shouldMapSelfApprovalToBiz210() {
      assertThat(mapper.fromLegacyError("SELF_APPROVAL")).contains(ErrorCode.ERR_BIZ_210);
    }

    @Test
    void shouldMapReversalWindowExpiredToBiz301() {
      assertThat(mapper.fromLegacyError("REVERSAL_WINDOW_EXPIRED")).contains(ErrorCode.ERR_BIZ_301);
    }

    @Test
    void shouldMapAlreadyReversedToBiz302() {
      assertThat(mapper.fromLegacyError("ALREADY_REVERSED")).contains(ErrorCode.ERR_BIZ_302);
    }

    @Test
    void shouldMapCbsTimeoutToExt101() {
      assertThat(mapper.fromLegacyError("CBS_TIMEOUT")).contains(ErrorCode.ERR_EXT_101);
    }

    @Test
    void shouldMapCbsErrorToExt102() {
      assertThat(mapper.fromLegacyError("CBS_ERROR")).contains(ErrorCode.ERR_EXT_102);
    }

    @Test
    void shouldMapBillerTimeoutToExt201() {
      assertThat(mapper.fromLegacyError("BILLER_TIMEOUT")).contains(ErrorCode.ERR_EXT_201);
    }

    @Test
    void shouldMapBillerErrorToExt202() {
      assertThat(mapper.fromLegacyError("BILLER_ERROR")).contains(ErrorCode.ERR_EXT_202);
    }

    @Test
    void shouldMapJpnUnavailableToExt301() {
      assertThat(mapper.fromLegacyError("JPN_UNAVAILABLE")).contains(ErrorCode.ERR_EXT_301);
    }

    @Test
    void shouldMapBiometricUnavailableToExt302() {
      assertThat(mapper.fromLegacyError("BIOMETRIC_UNAVAILABLE")).contains(ErrorCode.ERR_EXT_302);
    }

    @Test
    void shouldMapBiometricMismatchToExt303() {
      assertThat(mapper.fromLegacyError("BIOMETRIC_MISMATCH")).contains(ErrorCode.ERR_EXT_303);
    }

    @Test
    void shouldMapPaynetTimeoutToExt401() {
      assertThat(mapper.fromLegacyError("PAYNET_TIMEOUT")).contains(ErrorCode.ERR_EXT_401);
    }

    @Test
    void shouldMapPaynetErrorToExt402() {
      assertThat(mapper.fromLegacyError("PAYNET_ERROR")).contains(ErrorCode.ERR_EXT_402);
    }

    @Test
    void shouldMapIsoSocketLostToExt403() {
      assertThat(mapper.fromLegacyError("ISO_SOCKET_LOST")).contains(ErrorCode.ERR_EXT_403);
    }

    @Test
    void shouldMapInvalidCredentialsToAuth001() {
      assertThat(mapper.fromLegacyError("INVALID_CREDENTIALS")).contains(ErrorCode.ERR_AUTH_001);
    }

    @Test
    void shouldMapMissingTokenToAuth002() {
      assertThat(mapper.fromLegacyError("MISSING_TOKEN")).contains(ErrorCode.ERR_AUTH_002);
    }

    @Test
    void shouldMapAuthServiceUnavailableToAuth003() {
      assertThat(mapper.fromLegacyError("AUTH_SERVICE_UNAVAILABLE")).contains(ErrorCode.ERR_AUTH_003);
    }

    @Test
    void shouldMapRateLimitExceededToAuth004() {
      assertThat(mapper.fromLegacyError("RATE_LIMIT_EXCEEDED")).contains(ErrorCode.ERR_AUTH_004);
    }

    @Test
    void shouldMapAccountLockedToAuth004() {
      assertThat(mapper.fromLegacyError("ACCOUNT_LOCKED")).contains(ErrorCode.ERR_AUTH_004);
    }

    @Test
    void shouldMapSanctionsBlockToAuth101() {
      assertThat(mapper.fromLegacyError("SANCTIONS_BLOCK")).contains(ErrorCode.ERR_AUTH_101);
    }

    @Test
    void shouldMapPinLockedToAuth102() {
      assertThat(mapper.fromLegacyError("PIN_LOCKED")).contains(ErrorCode.ERR_AUTH_102);
    }

    @Test
    void shouldMapTokenExpiredToAuth103() {
      assertThat(mapper.fromLegacyError("TOKEN_EXPIRED")).contains(ErrorCode.ERR_AUTH_103);
    }

    @Test
    void shouldMapInternalErrorToSys001() {
      assertThat(mapper.fromLegacyError("INTERNAL_ERROR")).contains(ErrorCode.ERR_SYS_001);
    }

    @Test
    void shouldMapGpsUnavailableToSys002() {
      assertThat(mapper.fromLegacyError("GPS_UNAVAILABLE")).contains(ErrorCode.ERR_SYS_002);
    }

    @Test
    void shouldMapTemporalCorruptionToSys003() {
      assertThat(mapper.fromLegacyError("TEMPORAL_CORRUPTION")).contains(ErrorCode.ERR_SYS_003);
    }

    @Test
    void shouldMapTemporalNotFoundToSys004() {
      assertThat(mapper.fromLegacyError("TEMPORAL_NOT_FOUND")).contains(ErrorCode.ERR_SYS_004);
    }

    @Test
    void shouldMapIsoEncodingFailedToIso001() {
      assertThat(mapper.fromLegacyError("ISO_ENCODING_FAILED")).contains(ErrorCode.ERR_ISO_001);
    }

    @Test
    void shouldMapIsoUnmarshalFailedToIso002() {
      assertThat(mapper.fromLegacyError("ISO_UNMARSHAL_FAILED")).contains(ErrorCode.ERR_ISO_002);
    }

    @Test
    void shouldMapStanExhaustedToIso003() {
      assertThat(mapper.fromLegacyError("STAN_EXHAUSTED")).contains(ErrorCode.ERR_ISO_003);
    }

    @Test
    void shouldMapSwitchConnectionFailedToIso002() {
      assertThat(mapper.fromLegacyError("SWITCH_CONNECTION_FAILED")).contains(ErrorCode.ERR_ISO_002);
    }

    @Test
    void shouldMapCbsSoapTimeoutToCbs001() {
      assertThat(mapper.fromLegacyError("CBS_SOAP_TIMEOUT")).contains(ErrorCode.ERR_CBS_001);
    }

    @Test
    void shouldMapCbsQueueEmptyToCbs002() {
      assertThat(mapper.fromLegacyError("CBS_QUEUE_EMPTY")).contains(ErrorCode.ERR_CBS_002);
    }

    @Test
    void shouldMapCbsFormatErrorToCbs003() {
      assertThat(mapper.fromLegacyError("CBS_FORMAT_ERROR")).contains(ErrorCode.ERR_CBS_003);
    }

    @Test
    void shouldMapHsmPinBlockFailedToHsm001() {
      assertThat(mapper.fromLegacyError("HSM_PIN_BLOCK_FAILED")).contains(ErrorCode.ERR_HSM_001);
    }

    @Test
    void shouldMapHsmPinVerifyFailedToHsm002() {
      assertThat(mapper.fromLegacyError("HSM_PIN_VERIFY_FAILED")).contains(ErrorCode.ERR_HSM_002);
    }

    @Test
    void shouldMapHsmKeyVaultViolationToHsm003() {
      assertThat(mapper.fromLegacyError("HSM_KEY_VAULT_VIOLATION")).contains(ErrorCode.ERR_HSM_003);
    }

    @Test
    void shouldMapHsmConnectionLostToHsm004() {
      assertThat(mapper.fromLegacyError("HSM_CONNECTION_LOST")).contains(ErrorCode.ERR_HSM_004);
    }

    @Test
    void shouldMapBillerApiKeyFailedToBg001() {
      assertThat(mapper.fromLegacyError("BILLER_API_KEY_FAILED")).contains(ErrorCode.ERR_BG_001);
    }

    @Test
    void shouldMapBillerIdempotencyConflictToBg002() {
      assertThat(mapper.fromLegacyError("BILLER_IDEMPOTENCY_CONFLICT")).contains(ErrorCode.ERR_BG_002);
    }

    @Test
    void shouldReturnEmptyForUnknownLegacyError() {
      assertThat(mapper.fromLegacyError("UNKNOWN_ERROR")).isEmpty();
    }
  }
}