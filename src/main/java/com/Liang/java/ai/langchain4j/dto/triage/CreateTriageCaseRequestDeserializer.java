package com.Liang.java.ai.langchain4j.dto.triage;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class CreateTriageCaseRequestDeserializer extends JsonDeserializer<CreateTriageCaseRequest> {
    @Override public CreateTriageCaseRequest deserialize(JsonParser p, DeserializationContext c) throws IOException {
        ObjectNode node = p.getCodec().readTree(p);
        Set<String> allowed = Set.of("chiefComplaint");
        var fields = node.fieldNames();
        while (fields.hasNext()) if (!allowed.contains(fields.next()))
            throw JsonMappingException.from(p, "请求包含不支持的字段");
        JsonNode complaint = node.get("chiefComplaint");
        return new CreateTriageCaseRequest(complaint == null || complaint.isNull() ? null : complaint.asText());
    }
}
