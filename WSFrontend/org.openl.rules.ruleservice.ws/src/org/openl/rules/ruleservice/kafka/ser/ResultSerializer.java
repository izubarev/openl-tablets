package org.openl.rules.ruleservice.kafka.ser;

import java.util.Map;

import org.apache.kafka.common.serialization.Serializer;
import org.openl.rules.ruleservice.core.OpenLService;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ResultSerializer implements Serializer<Object> {
    private final ObjectMapper objectMapper;
    private String encoding = "UTF8";

    // Do not remove first argument. It is used by reflection.
    public ResultSerializer(OpenLService service, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Object encodingValue = configs.get("value.serializer.encoding");
        if (encodingValue == null) {
            encodingValue = configs.get("serializer.encoding");
        }
        if (encodingValue instanceof String) {
            encoding = (String) encodingValue;
        }
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        try {
            if (data instanceof String) {
                return ((String) data).getBytes(encoding);
            }
            return objectMapper.writeValueAsString(data).getBytes(encoding);
        } catch (Exception e) {
            throw new SerializationException("Failed to write a result.", e);
        }
    }

    @Override
    public void close() {
    }
}
