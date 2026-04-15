package com.agentbanking.common.infrastructure.caching;

import com.agentbanking.common.domain.port.out.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
public class RedisIdempotencyAdapter implements IdempotencyService {

  private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyAdapter.class);
  private static final String KEY_PREFIX = "idempotency:";

  private final RedisTemplate<String, Object> redisTemplate;

  public RedisIdempotencyAdapter(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  @Override
  public boolean checkAndStore(String idempotencyKey, Object response, Duration ttl) {
    String key = KEY_PREFIX + idempotencyKey;
    
    Boolean result = redisTemplate.opsForValue().setIfAbsent(key, response, ttl);
    
    if (Boolean.TRUE.equals(result)) {
      log.info("Stored idempotent response for key: {}", idempotencyKey);
      return true;
    }
    
    log.warn("Duplicate request detected for key: {}", idempotencyKey);
    return false;
  }

  @Override
  public Object getResponse(String idempotencyKey) {
    String key = KEY_PREFIX + idempotencyKey;
    return redisTemplate.opsForValue().get(key);
  }

  @Override
  public boolean exists(String idempotencyKey) {
    String key = KEY_PREFIX + idempotencyKey;
    return Boolean.TRUE.equals(redisTemplate.hasKey(key));
  }
}