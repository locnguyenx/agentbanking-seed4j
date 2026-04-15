package com.agentbanking.prepaid.domain.service;

import com.agentbanking.UnitTest;
import com.agentbanking.prepaid.domain.port.out.TopUpGatewayPort;
import com.agentbanking.prepaid.domain.port.out.TopUpProviderRegistryPort;
import com.agentbanking.prepaid.domain.port.out.TopUpResult;
import com.agentbanking.prepaid.domain.model.TopUpProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@UnitTest
class PrepaidTopUpServiceTest {

    private TopUpGatewayPort gatewayPort;
    private TopUpProviderRegistryPort registryPort;
    private PhoneValidationService phoneValidationService;
    private PrepaidTopUpService service;

    @BeforeEach
    void setUp() {
        gatewayPort = mock(TopUpGatewayPort.class);
        registryPort = mock(TopUpProviderRegistryPort.class);
        phoneValidationService = new PhoneValidationService();
        service = new PrepaidTopUpService(gatewayPort, registryPort, phoneValidationService);
    }

    @Test
    void topUp_successful() {
        when(registryPort.findByCode("CELCOM")).thenReturn(Optional.of(TopUpProvider.CELCOM));
        when(gatewayPort.topUp(anyString(), anyString(), any(BigDecimal.class)))
            .thenReturn(new TopUpResult(true, "TXN-123", "CONF-456", null));

        TopUpResult result = service.topUp("CELCOM", "+60123456789", new BigDecimal("30"), "idem-123");

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).isEqualTo("TXN-123");
        verify(gatewayPort).topUp("CELCOM", "+60123456789", new BigDecimal("30"));
    }

    @Test
    void topUp_invalidProvider_throwsException() {
        when(registryPort.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.topUp("UNKNOWN", "+60123456789", new BigDecimal("30"), "idem"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown provider");
    }

    @Test
    void topUp_invalidAmount_throwsException() {
        when(registryPort.findByCode("CELCOM")).thenReturn(Optional.of(TopUpProvider.CELCOM));

        assertThatThrownBy(() -> service.topUp("CELCOM", "+60123456789", BigDecimal.ZERO, "idem"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("positive");
    }

    @Test
    void topUp_amountExceedsLimit_throwsException() {
        when(registryPort.findByCode("CELCOM")).thenReturn(Optional.of(TopUpProvider.CELCOM));

        assertThatThrownBy(() -> service.topUp("CELCOM", "+60123456789", new BigDecimal("500"), "idem"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds maximum");
    }

    @Test
    void topUp_invalidPhone_returnsErrorResult() {
        when(registryPort.findByCode("CELCOM")).thenReturn(Optional.of(TopUpProvider.CELCOM));

        TopUpResult result = service.topUp("CELCOM", "invalid", new BigDecimal("30"), "idem");

        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).contains("ERR_VAL_006");
    }
}