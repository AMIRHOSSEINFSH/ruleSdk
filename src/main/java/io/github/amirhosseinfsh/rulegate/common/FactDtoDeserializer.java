package io.github.amirhosseinfsh.rulegate.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.amirhosseinfsh.rulegate.domain.fact.FactDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Component
public class FactDtoDeserializer extends JsonDeserializer<FactDto> {

    private final ObjectMapper objectMapper;

    public FactDtoDeserializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public FactDto deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        
        JsonNode node = jp.getCodec().readTree(jp);

        return parseWrapperFactDto(node);

    }
    private FactDto parseWrapperFactDto(JsonNode node) throws IOException {
        return parseFactDeclareDto(node).getFirst();
    }

    private List<FactDto> parseFactDeclareDto(JsonNode pNode) throws IOException {
        List<FactDto> factAttribiuteDtoList = new ArrayList<>();
        ArrayNode arrayNode = null;
        if (pNode.isObject()) {
            ArrayNode jsonNodes = new ArrayNode(null);
            jsonNodes.add(pNode);
            arrayNode = jsonNodes;
        }
        if (pNode.isArray()) {
            arrayNode = (ArrayNode) pNode;
        }
        arrayNode.forEach(node-> {
            FactDto factDeclareDto = new FactDto();
            if (node.has("name")) {
                factDeclareDto.setName(node.get("name").asText());
            }
            if (node.has("type")) {
                factDeclareDto.setType(node.get("type").asText());
            }

            if (node.has("value")) {
                if (node.get("value") instanceof ArrayNode) {
                    try {
                        factDeclareDto.setValue(parseFactDeclareDto(node.get("value")));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }else {
                    try {
                        factDeclareDto.setValue(parseFactAttribute(node.get("value"),factDeclareDto.getType()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            factAttribiuteDtoList.add(factDeclareDto);
        });

        return factAttribiuteDtoList;
    }

    private Object parseFactAttribute(JsonNode node, String type) throws IOException {
        return parseValue(node, type);
    }

    private Object parseValue(JsonNode valueNode, String type) throws IOException {
        if (valueNode == null || valueNode.isNull()) {
            return null;
        }
        try {
            var clzz= Class.forName(type);
            return objectMapper.treeToValue(valueNode, clzz);
        } catch (ClassNotFoundException e) {

        }
        if (isComplexType(type)) {
            return objectMapper.convertValue(valueNode, LinkedHashMap.class);
        }

        // Handle primitive types
        if (valueNode.isTextual()) {
            return valueNode.asText();
        } else if (valueNode.isInt()) {
            return valueNode.asInt();
        } else if (valueNode.isLong()) {
            return valueNode.asLong();
        } else if (valueNode.isDouble() || valueNode.isFloat()) {
            return valueNode.asDouble();
        } else if (valueNode.isBoolean()) {
            return valueNode.asBoolean();
        } else if (valueNode.isArray()) {
            return objectMapper.convertValue(valueNode, List.class);
        } else if (valueNode.isObject()) {
            return objectMapper.convertValue(valueNode, LinkedHashMap.class);
        }

        return valueNode.asText();
    }

    private boolean isComplexType(String type) {
        if (type == null) {
            return false;
        }
        
        // Add your logic to determine if type represents a FactAttribiuteDto
        // For example:
        return type.equals("FactAttribiuteDto") 
            || type.equals("object") 
            || type.contains(".")
            || type.startsWith("io.github.amirhosseinfsh.ruleGate");
    }
}