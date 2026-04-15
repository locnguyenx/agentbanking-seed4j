package com.agentbanking.onboarding.domain.model;

import static org.assertj.core.api.Assertions.*;

import com.agentbanking.UnitTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
@DisplayName("KycVerification")
class KycVerificationTest {

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        void shouldCreateKycWithInProgressStatus() {
            UUID agentId = UUID.randomUUID();
            String mykadNumber = "900101011234";

            KycVerification kyc = KycVerification.create(agentId, mykadNumber);

            assertThat(kyc.id()).isNotNull();
            assertThat(kyc.agentId()).isEqualTo(agentId);
            assertThat(kyc.mykadNumber()).isEqualTo(mykadNumber);
            assertThat(kyc.status()).isEqualTo(KycStatus.IN_PROGRESS);
            assertThat(kyc.submittedAt()).isNotNull();
            assertThat(kyc.verifiedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        void shouldVerifyWithAllRequiredData() {
            KycVerification kyc = KycVerification.create(UUID.randomUUID(), "900101011234");

            KycVerification verified = kyc.verify(95, "CLEAN", true, 25);

            assertThat(verified.status()).isEqualTo(KycStatus.VERIFIED);
            assertThat(verified.livenessScore()).isEqualTo(95);
            assertThat(verified.amlStatus()).isEqualTo("CLEAN");
            assertThat(verified.biometricMatch()).isTrue();
            assertThat(verified.age()).isEqualTo(25);
            assertThat(verified.verifiedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("isAutoApprovalEligible")
    class AutoApprovalEligible {

        @Test
        void shouldBeEligibleWhenBiometricMatchYesAmlCleanAndAge18Plus() {
            KycVerification kyc = KycVerification.create(UUID.randomUUID(), "900101011234")
                .verify(95, "CLEAN", true, 25);

            assertThat(kyc.isAutoApprovalEligible()).isTrue();
        }

        @Test
        void shouldNotBeEligibleWhenBiometricMatchNo() {
            KycVerification kyc = KycVerification.create(UUID.randomUUID(), "900101011234")
                .verify(95, "CLEAN", false, 25);

            assertThat(kyc.isAutoApprovalEligible()).isFalse();
        }

        @Test
        void shouldNotBeEligibleWhenAmlNotClean() {
            KycVerification kyc = KycVerification.create(UUID.randomUUID(), "900101011234")
                .verify(95, "FLAGGED", true, 25);

            assertThat(kyc.isAutoApprovalEligible()).isFalse();
        }

        @Test
        void shouldNotBeEligibleWhenAgeUnder18() {
            KycVerification kyc = KycVerification.create(UUID.randomUUID(), "900101011234")
                .verify(95, "CLEAN", true, 17);

            assertThat(kyc.isAutoApprovalEligible()).isFalse();
        }

        @Test
        void shouldNotBeEligibleWhenAgeIs18() {
            KycVerification kyc = KycVerification.create(UUID.randomUUID(), "900101011234")
                .verify(95, "CLEAN", true, 18);

            assertThat(kyc.isAutoApprovalEligible()).isTrue();
        }

        @Test
        void shouldNotBeEligibleWhenBiometricMatchNull() {
            KycVerification kyc = KycVerification.create(UUID.randomUUID(), "900101011234")
                .verify(95, "CLEAN", null, 25);

            assertThat(kyc.isAutoApprovalEligible()).isFalse();
        }

        @Test
        void shouldNotBeEligibleWhenAmlStatusNull() {
            KycVerification kyc = KycVerification.create(UUID.randomUUID(), "900101011234")
                .verify(95, null, true, 25);

            assertThat(kyc.isAutoApprovalEligible()).isFalse();
        }

        @Test
        void shouldNotBeEligibleWhenAgeNull() {
            KycVerification kyc = KycVerification.create(UUID.randomUUID(), "900101011234")
                .verify(95, "CLEAN", true, null);

            assertThat(kyc.isAutoApprovalEligible()).isFalse();
        }
    }

    @Nested
    @DisplayName("reject")
    class Reject {

        @Test
        void shouldRejectWithReason() {
            KycVerification kyc = KycVerification.create(UUID.randomUUID(), "900101011234")
                .verify(95, "CLEAN", true, 25);

            KycVerification rejected = kyc.reject("Biometric mismatch");

            assertThat(rejected.status()).isEqualTo(KycStatus.REJECTED);
            assertThat(rejected.rejectionReason()).isEqualTo("Biometric mismatch");
        }
    }
}