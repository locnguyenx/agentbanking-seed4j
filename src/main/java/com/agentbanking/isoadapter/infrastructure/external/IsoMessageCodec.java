package com.agentbanking.isoadapter.infrastructure.external;

import com.agentbanking.isoadapter.domain.model.IsoMessage;
import com.agentbanking.isoadapter.domain.model.IsoMessageType;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

public class IsoMessageCodec {

    private static final byte[] STX = {0x02};
    private static final byte[] ETX = {0x03};
    private static final byte[] FS = {0x1C};

    public byte[] encode(IsoMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append(message.mti());
        
        Map<Integer, String> fields = message.fields();
        for (Map.Entry<Integer, String> entry : fields.entrySet()) {
            sb.append(FS[0]).append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        byte[] payload = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bitmap = message.bitmap();
        
        byte[] result = new byte[STX.length + payload.length + ETX.length + bitmap.length + 2];
        System.arraycopy(STX, 0, result, 0, 1);
        System.arraycopy(payload, 0, result, 1, payload.length);
        System.arraycopy(ETX, 0, result, 1 + payload.length, 1);
        System.arraycopy(bitmap, 0, result, 1 + payload.length + 1, bitmap.length);
        
        return result;
    }

    public IsoMessage decode(byte[] data) {
        if (data == null || data.length < 5) {
            throw new IllegalArgumentException("ERR_ISO_001: Invalid ISO message length");
        }

        int stxIndex = -1;
        int etxIndex = -1;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == 0x02) stxIndex = i;
            if (data[i] == 0x03) {
                etxIndex = i;
                break;
            }
        }

        if (stxIndex == -1 || etxIndex == -1) {
            throw new IllegalArgumentException("ERR_ISO_002: Missing STX/ETX delimiters");
        }

        int payloadStart = stxIndex + 1;
        int payloadLength = etxIndex - payloadStart;
        String payload = new String(data, payloadStart, payloadLength, StandardCharsets.UTF_8);
        
        String[] parts = new String(payload).split(String.valueOf((char)0x1C));
        if (parts.length == 0) {
            throw new IllegalArgumentException("ERR_ISO_003: Empty payload");
        }

        String mti = parts[0];
        IsoMessage.IsoMessageBuilder builder = IsoMessage.builder(mti);

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int eqIndex = part.indexOf('=');
            if (eqIndex > 0) {
                int fieldNum = Integer.parseInt(part.substring(0, eqIndex));
                String value = part.substring(eqIndex + 1);
                builder.field(fieldNum, value);
            }
        }

        return builder.build();
    }

    public String extractResponseCode(IsoMessage message) {
        return message.getField(39);
    }

    public String extractStan(IsoMessage message) {
        return message.getField(11);
    }
}