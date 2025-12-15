package com.gestortransito.modulos.cruzamento.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * Custom deserializer that converts JSON strings to Maps.
 * Handles both Spring-serialized messages (with type headers) and plain Python JSON messages.
 */
public class JsonDeserializer implements Deserializer<Map<String, Object>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            String jsonString = new String(data, "UTF-8");
            return objectMapper.readValue(jsonString, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON to Map", e);
        }
    }
}
