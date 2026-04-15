package com.agentbanking.billeradapter.infrastructure.web;

import com.agentbanking.billeradapter.application.dto.*;
import com.agentbanking.billeradapter.application.service.BillerApplicationService;
import com.agentbanking.billeradapter.domain.model.Biller;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billers")
public class BillerController {

  private static final Logger log = LoggerFactory.getLogger(BillerController.class);

  private final BillerApplicationService applicationService;

  public BillerController(BillerApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @PostMapping("/inquire")
  public ResponseEntity<BillInquiryResponse> inquireBill(@Valid @RequestBody BillInquiryRequest request) {
    log.info("Bill inquiry request: biller={}, ref1={}", request.billerCode(), request.ref1());
    return ResponseEntity.ok(applicationService.inquireBill(request));
  }

  @PostMapping("/pay")
  public ResponseEntity<BillPaymentResponse> payBill(@Valid @RequestBody BillPaymentRequest request) {
    log.info("Bill payment request: biller={}, ref1={}, amount={}",
        request.billerCode(), request.ref1(), request.amount());
    return ResponseEntity.ok(applicationService.payBill(request));
  }

  @GetMapping
  public ResponseEntity<List<Biller>> listBillers() {
    log.info("List all billers request");
    return ResponseEntity.ok(List.of());
  }
}