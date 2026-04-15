package com.agentbanking.isoadapter.infrastructure.external;

import com.agentbanking.isoadapter.domain.model.IsoMessage;
import feign.RequestTemplate;
import feign.codec.Encoder;

import java.lang.reflect.Type;

public class IsoMessageEncoder implements Encoder {

    private final IsoMessageCodec codec = new IsoMessageCodec();

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) {
        if (!(object instanceof IsoMessage message)) {
            throw new RuntimeException("ERR_ISO_102: Invalid request type");
        }

        byte[] encoded = codec.encode(message);
        template.body(encoded, java.nio.charset.StandardCharsets.ISO_8859_1);
    }
}