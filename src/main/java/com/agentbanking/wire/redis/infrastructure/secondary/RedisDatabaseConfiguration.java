package com.agentbanking.wire.redis.infrastructure.secondary;

import com.agentbanking.wire.redis.infrastructure.secondary.JSR310DateConverters.DateToZonedDateTimeConverter;
import com.agentbanking.wire.redis.infrastructure.secondary.JSR310DateConverters.ZonedDateTimeToDateConverter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.convert.RedisCustomConversions;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories({ "com.agentbanking" })
class RedisDatabaseConfiguration {

  @Bean
  public RedisCustomConversions redisCustomConversions() {
    return new RedisCustomConversions(List.of(DateToZonedDateTimeConverter.INSTANCE, ZonedDateTimeToDateConverter.INSTANCE));
  }
}
