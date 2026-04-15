package com.agentbanking.switchadapter.domain.service;

import com.agentbanking.UnitTest;
import com.agentbanking.switchadapter.application.dto.InternalTransactionRequest;
import com.agentbanking.switchadapter.application.dto.IsoTransactionResponse;
import com.agentbanking.switchadapter.domain.model.IsoMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@UnitTest
class IsoTranslationServiceTest {

    private IsoTranslationService service;

    @BeforeEach
    void setUp() {
        service = new IsoTranslationService();
    }

    @Test
    @DisplayName("should translate withdrawal request to ISO 8583")
    void shouldTranslateWithdrawalToIso() {
        InternalTransactionRequest request = new InternalTransactionRequest(
            "CASH_WITHDRAWAL", "4111111111111111", new BigDecimal("500.00"),
            "TERM001", "MERCH001", null, "ACQ001"
        );

        IsoMessage result = service.translate(request);

        assertEquals("0100", result.mti());
        assertNotNull(result.fields().get(2));
        assertEquals("00", result.fields().get(3));
    }

    @Test
    @DisplayName("should translate ISO response with approved code")
    void shouldTranslateApprovedResponse() {
        IsoMessage message = new IsoMessage("0110", Map.of(39, "00", 11, "000001", 37, "RRN001"), null);

        IsoTransactionResponse result = service.translateFromIso(message);

        assertTrue(result.authorized());
        assertEquals("00", result.responseCode());
        assertEquals("000001", result.stan());
    }

    @Test
    @DisplayName("should translate ISO response with decline code")
    void shouldTranslateDeclinedResponse() {
        IsoMessage message = new IsoMessage("0110", Map.of(39, "51", 11, "000001", 37, "RRN001"), null);

        IsoTransactionResponse result = service.translateFromIso(message);

        assertFalse(result.authorized());
        assertEquals("51", result.responseCode());
        assertEquals("Insufficient Funds", result.declineReason());
    }

    @Test
    @DisplayName("should translate deposit request to ISO 8583")
    void shouldTranslateDepositToIso() {
        InternalTransactionRequest request = new InternalTransactionRequest(
            "CASH_DEPOSIT", "4111111111111111", new BigDecimal("1000.00"),
            "TERM001", "MERCH001", null, "ACQ001"
        );

        IsoMessage result = service.translate(request);

        assertEquals("0100", result.mti());
        assertEquals("01", result.fields().get(3));
    }

    @Test
    @DisplayName("should translate reversal request to ISO 8583")
    void shouldTranslateReversalToIso() {
        InternalTransactionRequest request = new InternalTransactionRequest(
            "REVERSAL", "4111111111111111", new BigDecimal("500.00"),
            "TERM001", "MERCH001", "RRN123", "ACQ001"
        );

        IsoMessage result = service.translate(request);

        assertEquals("0400", result.mti());
    }
}