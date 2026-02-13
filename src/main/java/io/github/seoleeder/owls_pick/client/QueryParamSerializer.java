package io.github.seoleeder.owls_pick.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueryParamSerializer {
    private final ObjectMapper objectMapper;

    /**
     * DTO -> JSON 변화 로직
     * */
    public String serialize(Object dto){
        try{
            return objectMapper.writeValueAsString(dto);
        }catch (JsonProcessingException e) {
            log.error("Failed to serialize DTO to JSON: {}", dto, e);
            throw new RuntimeException("JSON Parameter Serialization Failed", e);
        }
    }
}
