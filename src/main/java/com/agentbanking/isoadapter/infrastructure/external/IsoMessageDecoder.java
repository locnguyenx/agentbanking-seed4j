package com.agentbanking.isoadapter.infrastructure.external;

import com.agentbanking.isoadapter.domain.model.IsoMessage;
import com.agentbanking.isoadapter.infrastructure.external.dto.IsoTransactionResponse;
import feign.FeignException;
import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class IsoMessageDecoder implements Decoder {

    private final IsoMessageCodec codec = new IsoMessageCodec();

    @Override
    public Object decode(Response response, Type bodyType) throws IOException {
        if (response.body() == null) {
            throw FeignException.errorStatus("ERR_ISO_104: Empty response body", response);
        }

        try {
            byte[] body = response.body().asInputStream().readAllBytes();
            if (body.length == 0) {
                throw FeignException.errorStatus("ERR_ISO_105: Empty response", response);
            }

            IsoMessage message = codec.decode(body);
            String responseCode = codec.extractResponseCode(message);
            String stan = codec.extractStan(message);

            return new IsoTransactionResponse(responseCode, "00".equals(responseCode), stan, new String(body, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw FeignException.errorStatus("ERR_ISO_106: " + e.getMessage(), response);
        }
    }
}