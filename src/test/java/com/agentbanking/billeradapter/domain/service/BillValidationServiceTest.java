package com.agentbanking.billeradapter.domain.service;

import com.agentbanking.UnitTest;
import com.agentbanking.billeradapter.domain.model.Biller;
import com.agentbanking.billeradapter.domain.model.BillType;
import com.agentbanking.billeradapter.domain.port.out.BillerRegistryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@UnitTest
class BillValidationServiceTest {

    private BillerRegistryPort registryPort;
    private BillValidationService service;

    @BeforeEach
    void setUp() {
        registryPort = mock(BillerRegistryPort.class);
        service = new BillValidationService(registryPort);
    }

    @Test
    void validateRef_validTnbRef_returnsValid() {
        when(registryPort.findByCode("TNB"))
            .thenReturn(Optional.of(new Biller("TNB", "Tenaga", BillType.UTILITY, "http://tnb.com", true, false)));

        BillValidationService.Result result = service.validateRef("TNB", "123456789012");

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateRef_invalidTnbRef_returnsInvalid() {
        when(registryPort.findByCode("TNB"))
            .thenReturn(Optional.of(new Biller("TNB", "Tenaga", BillType.UTILITY, "http://tnb.com", true, false)));

        BillValidationService.Result result = service.validateRef("TNB", "INVALID");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid reference format");
    }

    @Test
    void validateRef_validJompayRef_returnsValid() {
        when(registryPort.findByCode("JOMPAY"))
            .thenReturn(Optional.of(new Biller("JOMPAY", "JomPAY", BillType.OTHER, "http://jompay.com", true, false)));

        BillValidationService.Result result = service.validateRef("JOMPAY", "ABC1234567890");

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateRef_unknownBiller_returnsInvalid() {
        when(registryPort.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        BillValidationService.Result result = service.validateRef("UNKNOWN", "123");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Unknown biller");
    }

    @Test
    void validateRef_emptyRef_returnsInvalid() {
        when(registryPort.findByCode("TNB"))
            .thenReturn(Optional.of(new Biller("TNB", "Tenaga", BillType.UTILITY, "http://tnb.com", true, false)));

        BillValidationService.Result result = service.validateRef("TNB", "");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("required");
    }
}