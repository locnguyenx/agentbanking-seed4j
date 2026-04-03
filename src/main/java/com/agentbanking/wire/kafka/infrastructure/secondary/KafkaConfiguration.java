package com.agentbanking.wire.kafka.infrastructure.secondary;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class KafkaConfiguration {

  private static final Logger log = LoggerFactory.getLogger(KafkaConfiguration.class);

  @Value("${kafka.bootstrap-servers:localhost:9092}")
  private String bootstrapServers;

  @Value("${kafka.consumer.[group.id]:myapp}")
  private String groupId;

  @Value("${kafka.consumer.[auto.offset.reset]:earliest}")
  private String autoOffsetReset;

  @Value("${kafka.polling.timeout:10000}")
  private int pollingTimeout;

  @Bean
  Map<String, Object> kafkaConsumerConfigs() {
    var props = new HashMap<String, Object>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    log.info("Configured Kafka Consumer configs with bootstrap servers: {}", bootstrapServers);
    return props;
  }

  @Bean
  Map<String, Object> kafkaProducerConfigs() {
    var props = new HashMap<String, Object>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    log.info("Configured Kafka Producer configs with bootstrap servers: {}", bootstrapServers);
    return props;
  }
}
