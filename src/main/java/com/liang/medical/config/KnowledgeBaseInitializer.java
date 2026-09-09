package com.liang.medical.config;

import com.liang.medical.knowledge.dto.KnowledgeReloadResponse;
import com.liang.medical.knowledge.KnowledgeSeedLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Loads the versioned knowledge seeds at application startup.
 */
@Component
public class KnowledgeBaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseInitializer.class);

    private final KnowledgeSeedLoader knowledgeSeedLoader;

    public KnowledgeBaseInitializer(KnowledgeSeedLoader knowledgeSeedLoader) {
        this.knowledgeSeedLoader = knowledgeSeedLoader;
    }

    @Override
    public void run(String... args) {
        try {
            KnowledgeReloadResponse response = knowledgeSeedLoader.reload();
            log.info("知识库初始化完成，共加载 {} 个文档到向量存储，版本 {}",
                    response.documentsLoaded(), response.knowledgeVersion());
        } catch (Exception exception) {
            log.error("知识库初始化失败: {}", exception.getMessage(), exception);
        }
    }
}
