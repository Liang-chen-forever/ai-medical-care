package com.liang.medical.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.junit.jupiter.api.Test;
import com.liang.medical.knowledge.entity.KnowledgeDocument;
import com.liang.medical.knowledge.KnowledgeDocumentStatus;
import com.liang.medical.knowledge.mapper.KnowledgeDocumentMapper;

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

    @Test
    void reloadAlsoEmbedsPublishedManagedDocumentsWithoutInventingTriageMetadata() {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.embedAll(anyList())).thenAnswer(invocation -> {
            List<TextSegment> segments = invocation.getArgument(0);
            return Response.from(segments.stream()
                    .map(segment -> Embedding.from(new float[]{1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F}))
                    .toList());
        });
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        KnowledgeDocument managed = new KnowledgeDocument();
        managed.setId(9L);
        managed.setDocumentKey("triage-policy");
        managed.setDocumentName("triage-policy.md");
        managed.setContentText("人工审核流程");
        managed.setVersionNo(3);
        managed.setStatus(KnowledgeDocumentStatus.PUBLISHED);
        when(documentMapper.findAllPublished()).thenReturn(List.of(managed));

        KnowledgeSeedLoader loader = new KnowledgeSeedLoader(new KnowledgeSeedCatalog(), embeddingStore,
                embeddingModel, documentMapper);

        loader.reload();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[]{1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F}))
                .maxResults(20)
                .build()).matches();
        assertThat(matches.stream().map(match -> match.embedded().text()))
                .contains("人工审核流程");
        TextSegment managedSegment = matches.stream()
                .map(EmbeddingMatch::embedded)
                .filter(segment -> "managed-triage-policy-v3"
                        .equals(segment.metadata().getString(KnowledgeSeedCatalog.DOCUMENT_ID)))
                .findFirst()
                .orElseThrow();
        assertThat(managedSegment.metadata().getString(KnowledgeSeedCatalog.DEPARTMENT)).isBlank();
        assertThat(managedSegment.metadata().getString(KnowledgeSeedCatalog.KNOWLEDGE_VERSION))
                .isEqualTo("managed-3");
    }
}
