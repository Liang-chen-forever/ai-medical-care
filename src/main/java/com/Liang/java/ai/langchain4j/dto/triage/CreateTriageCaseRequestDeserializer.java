package com.Liang.java.ai.langchain4j.dto.triage;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CreateTriageCaseRequestDeserializer extends JsonDeserializer<CreateTriageCaseRequest> {
    @Override public CreateTriageCaseRequest deserialize(JsonParser p, DeserializationContext c) throws IOException {
        JsonNode parsed = p.getCodec().readTree(p);
        if (parsed == null || !parsed.isObject()) throw JsonMappingException.from(p, "请求体必须为JSON对象");
        ObjectNode node = (ObjectNode) parsed;
        Set<String> allowed = Set.of("chiefComplaint");
        var fields = node.fieldNames();
        while (fields.hasNext()) if (!allowed.contains(fields.next()))
            throw JsonMappingException.from(p, "请求包含不支持的字段");
        JsonNode complaint = node.get("chiefComplaint");
        return new CreateTriageCaseRequest(complaint == null || complaint.isNull() ? null : complaint.asText());
    }
}
