package com.Liang.java.ai.langchain4j.triage;

import com.Liang.java.ai.langchain4j.knowledge.KnowledgeSeedCatalog;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LangChain4jTriageEvidenceRetrieverTest {
    @Mock ContentRetriever contentRetriever;

    @Test
    void adapterCopiesScoreAndStableMetadataFromLangChainContent() {
        TextSegment segment = TextSegment.from("头痛建议前往神经内科", Metadata.from(Map.of(
                KnowledgeSeedCatalog.DOCUMENT_ID, "neurology-overview",
                KnowledgeSeedCatalog.DEPARTMENT, "神经内科",
                KnowledgeSeedCatalog.KNOWLEDGE_VERSION, "2026.09")));
        when(contentRetriever.retrieve(Query.from("反复头痛"))).thenReturn(List.of(
                Content.from(segment, Map.of(ContentMetadata.SCORE, 0.88))));

        RetrievedEvidence evidence = new LangChain4jTriageEvidenceRetriever(contentRetriever).retrieve("反复头痛").get(0);

        assertThat(evidence.documentId()).isEqualTo("neurology-overview");
        assertThat(evidence.department()).isEqualTo("神经内科");
        assertThat(evidence.knowledgeVersion()).isEqualTo("2026.09");
        assertThat(evidence.chunkId()).matches("[a-f0-9]{64}");
        assertThat(evidence.score()).isEqualTo(0.88);
        verify(contentRetriever).retrieve(Query.from("反复头痛"));
    }
}
