package com.agentbanking.common.infrastructure.primary;

import com.agentbanking.common.domain.port.out.IdempotencyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/idempotency")
class IdempotencyController {

  private final IdempotencyService idempotencyService;

  public IdempotencyController(IdempotencyService idempotencyService) {
    this.idempotencyService = idempotencyService;
  }

  @GetMapping("/{key}")
  public ResponseEntity<Object> get(@PathVariable String key) {
    Object response = idempotencyService.getResponse(key);
    if (response != null) {
      return ResponseEntity.ok(response);
    }
    return ResponseEntity.notFound().build();
  }

  @DeleteMapping("/{key}")
  public ResponseEntity<Map<String, Boolean>> delete(@PathVariable String key) {
    return ResponseEntity.ok(Map.of("deleted", true));
  }
}