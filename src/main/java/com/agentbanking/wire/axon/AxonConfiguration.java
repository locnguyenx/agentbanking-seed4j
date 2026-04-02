package com.agentbanking.wire.axon;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.NoTypePermission;
import org.axonframework.config.Configuration;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.xml.XStreamSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
class AxonConfiguration {

  @Bean
  Serializer defaultSerializer() {
    XStream xStream = new XStream();
    xStream.addPermission(NoTypePermission.NONE);
    xStream.allowTypesByWildcard(new String[] {
      "com.agentbanking.**",
      "org.axonframework.**",
      "java.**"
    });
    return XStreamSerializer.builder().xStream(xStream).build();
  }

  @Autowired
  void configureEventProcessing(EventProcessingConfigurer configurer) {
    configurer.usingSubscribingEventProcessors();
  }
}
