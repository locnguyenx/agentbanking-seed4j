package com.agentbanking.hsmadapter.domain.model;

public record PinTranslationResult(
  boolean success,
  String translatedPin,
  String errorCode
) {}
