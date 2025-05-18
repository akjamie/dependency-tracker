package org.akj.test.tracker.infrastructure.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfig {
    @Bean("orderedObjectMapper")
    public ObjectMapper orderedObjectMapper() {
        ObjectMapper objectMapper =
                JsonMapper.builder()
                        .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .build();
        return objectMapper;
    }
}
