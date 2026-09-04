package com.Liang.java.ai.langchain4j.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeSeedLoaderTest {

    @Test
    void reloadEmbedsCatalogDocumentsWithConfiguredModelAndMakesThemSearchable() {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.embedAll(anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(0);
            return Response.from(segments.stream()
                    .map(segment -> Embedding.from(new float[]{1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F}))
                    .toList());
        });
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        KnowledgeSeedCatalog catalog = new KnowledgeSeedCatalog();

        KnowledgeSeedLoader loader = new KnowledgeSeedLoader(catalog, embeddingStore, embeddingModel);

        loader.reload();

        verify(embeddingModel).embedAll(anyList());
        assertThat(embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[]{1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F}))
                .maxResults(10)
                .build()).matches())
                .isNotEmpty();
    }
}
