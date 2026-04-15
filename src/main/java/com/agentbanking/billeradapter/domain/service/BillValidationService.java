package com.agentbanking.billeradapter.domain.service;

import com.agentbanking.billeradapter.domain.model.Biller;
import com.agentbanking.billeradapter.domain.port.out.BillerRegistryPort;
import java.util.regex.Pattern;

public class BillValidationService {

  private static final Pattern TNB_REF_PATTERN = Pattern.compile("^\\d{12,14}$");
  private static final Pattern MAXIS_REF_PATTERN = Pattern.compile("^\\d{10,12}$");
  private static final Pattern JOMPAY_REF_PATTERN = Pattern.compile("^[A-Z0-9]{10,20}$");
  private static final Pattern EPF_REF_PATTERN = Pattern.compile("^\\d{10,14}$");

  private final BillerRegistryPort billerRegistryPort;

  public BillValidationService(BillerRegistryPort billerRegistryPort) {
    this.billerRegistryPort = billerRegistryPort;
  }

  public Result validateRef(String billerCode, String ref1) {
    return billerRegistryPort.findByCode(billerCode)
        .map(biller -> doValidate(biller, ref1))
        .orElse(Result.invalid("Unknown biller: " + billerCode));
  }

  private Result doValidate(Biller biller, String ref1) {
    if (ref1 == null || ref1.isBlank()) {
      return Result.invalid("Reference number is required");
    }

    return switch (biller.billerCode().toUpperCase()) {
      case "TNB" -> validateByPattern(TNB_REF_PATTERN, ref1, "TNB");
      case "MAXIS", "DIGI", "UNIFI" -> validateByPattern(MAXIS_REF_PATTERN, ref1, "Telecom");
      case "JOMPAY" -> validateByPattern(JOMPAY_REF_PATTERN, ref1, "JomPAY");
      case "EPF" -> validateByPattern(EPF_REF_PATTERN, ref1, "EPF");
      case "ASTRO" -> validateByPattern(MAXIS_REF_PATTERN, ref1, "Astro");
      case "TM" -> validateByPattern(MAXIS_REF_PATTERN, ref1, "TM");
      default -> Result.valid();
    };
  }

  private Result validateByPattern(Pattern pattern, String ref, String billerName) {
    if (pattern.matcher(ref).matches()) {
      return Result.valid();
    }
    return Result.invalid("Invalid reference format for " + billerName);
  }

  public static class Result {
    private final boolean valid;
    private final String errorMessage;

    public Result(boolean valid, String errorMessage) {
      this.valid = valid;
      this.errorMessage = errorMessage;
    }

    public static Result valid() {
      return new Result(true, null);
    }

    public static Result invalid(String message) {
      return new Result(false, message);
    }

    public boolean isValid() { return valid; }
    public String getErrorMessage() { return errorMessage; }
  }
}