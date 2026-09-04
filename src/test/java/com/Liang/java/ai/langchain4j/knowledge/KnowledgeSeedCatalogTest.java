package com.Liang.java.ai.langchain4j.knowledge;

import dev.langchain4j.data.document.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeSeedCatalogTest {

    @Test
    void bundledSeedsCarryStableTriageMetadata() {
        List<Document> documents = new KnowledgeSeedCatalog().loadDocuments();

        assertThat(documents).hasSize(4);
        assertThat(documents).allSatisfy(document -> {
            assertThat(document.metadata().getString(KnowledgeSeedCatalog.DOCUMENT_ID)).isNotBlank();
            assertThat(document.metadata().getString(KnowledgeSeedCatalog.KNOWLEDGE_VERSION)).isEqualTo("2026.09");
        });
        assertThat(documents).anySatisfy(document ->
                assertThat(document.metadata().getString(KnowledgeSeedCatalog.DEPARTMENT)).isEqualTo("神经内科"));
    }
}
