package com.liang.medical.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRedisPropertiesTest {

    @Test
    void bindsEveryKnowledgeRedisSetting() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "app.knowledge.redis.host", "redis.internal",
                "app.knowledge.redis.port", "6381",
                "app.knowledge.redis.index-name", "medical-knowledge",
                "app.knowledge.redis.prefix", "knowledge:vectors:",
                "app.knowledge.redis.dimension", "8"));

        KnowledgeRedisProperties properties = new Binder(source)
                .bind("app.knowledge.redis", Bindable.of(KnowledgeRedisProperties.class))
                .get();

        assertThat(properties.getHost()).isEqualTo("redis.internal");
        assertThat(properties.getPort()).isEqualTo(6381);
        assertThat(properties.getIndexName()).isEqualTo("medical-knowledge");
        assertThat(properties.getPrefix()).isEqualTo("knowledge:vectors:");
        assertThat(properties.getDimension()).isEqualTo(8);
    }
}
