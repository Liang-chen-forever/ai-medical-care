package com.liang.medical.triage;

import com.liang.medical.knowledge.KnowledgeSeedCatalog;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
public class LangChain4jTriageEvidenceRetriever implements TriageEvidenceRetriever {
    private final ContentRetriever contentRetriever;

    public LangChain4jTriageEvidenceRetriever(@Qualifier("contentRetrieverXiaozhiPincone") ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    @Override
    public List<RetrievedEvidence> retrieve(String chiefComplaint) {
        return contentRetriever.retrieve(Query.from(chiefComplaint)).stream().map(this::toEvidence).toList();
    }

    private RetrievedEvidence toEvidence(Content content) {
        TextSegment segment = content.textSegment();
        Metadata metadata = segment.metadata();
        Object rawScore = content.metadata().get(ContentMetadata.SCORE);
        double score = rawScore instanceof Number number ? number.doubleValue() : Double.NaN;
        String documentId = metadata.getString(KnowledgeSeedCatalog.DOCUMENT_ID);
        return new RetrievedEvidence(documentId, sha256(documentId + "\n" + segment.text()),
                metadata.getString(KnowledgeSeedCatalog.DEPARTMENT),
                metadata.getString(KnowledgeSeedCatalog.KNOWLEDGE_VERSION), truncate(segment.text(), 500), score);
    }

    private String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
