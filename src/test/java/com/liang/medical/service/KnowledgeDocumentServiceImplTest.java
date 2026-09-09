package com.liang.medical.service;

import com.liang.medical.knowledge.service.KnowledgeDocumentService;
import com.liang.medical.common.BusinessException;
import com.liang.medical.knowledge.entity.KnowledgeDocument;
import com.liang.medical.knowledge.KnowledgeDocumentStatus;
import com.liang.medical.knowledge.mapper.KnowledgeDocumentMapper;
import com.liang.medical.knowledge.service.KnowledgeDocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeDocumentServiceImplTest {
    private KnowledgeDocumentMapper documentMapper;
    private KnowledgeDocumentService service;

    @BeforeEach
    void setUp() {
        documentMapper = mock(KnowledgeDocumentMapper.class);
        service = new KnowledgeDocumentServiceImpl(documentMapper);
    }

    @Test
    void uploadDeduplicatesBySha256AndDoesNotInsertAgain() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "triage.md", "text/markdown",
                "头痛建议".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        KnowledgeDocument existing = new KnowledgeDocument();
        existing.setId(9L);
        existing.setContentSha256("existing");
        when(documentMapper.findByContentSha256(any(String.class))).thenReturn(existing);

        assertThat(service.upload(1L, file)).isSameAs(existing);
        verify(documentMapper, never()).insert(any(KnowledgeDocument.class));
    }

    @Test
    void uploadRejectsNonTextAndOversizedFiles() {
        MockMultipartFile binary = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[]{1});
        assertThatThrownBy(() -> service.upload(1L, binary))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仅支持 UTF-8 文本或 Markdown 文件");

        MockMultipartFile oversized = new MockMultipartFile("file", "x.md", "text/markdown",
                new byte[2 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> service.upload(1L, oversized))
                .isInstanceOf(BusinessException.class)
                .hasMessage("知识文档不能超过2MiB");
    }

    @Test
    void publishAndRollbackKeepVersionHistory() {
        KnowledgeDocument draft = document(10L, "triage", 2, KnowledgeDocumentStatus.DRAFT);
        KnowledgeDocument current = document(8L, "triage", 1, KnowledgeDocumentStatus.PUBLISHED);
        when(documentMapper.selectById(10L)).thenReturn(draft);
        when(documentMapper.findPublishedByDocumentKey("triage")).thenReturn(current);
        when(documentMapper.archivePublished("triage")).thenReturn(1);
        when(documentMapper.markPublished(10L)).thenReturn(1);

        assertThat(service.publish(1L, 10L).getStatus()).isEqualTo(KnowledgeDocumentStatus.PUBLISHED);
        verify(documentMapper).archivePublished("triage");
        verify(documentMapper).markPublished(10L);

        KnowledgeDocument archived = document(8L, "triage", 1, KnowledgeDocumentStatus.ARCHIVED);
        when(documentMapper.selectById(8L)).thenReturn(archived);
        when(documentMapper.findPublishedByDocumentKey("triage")).thenReturn(draft);
        when(documentMapper.archivePublished("triage")).thenReturn(1);
        when(documentMapper.markPublished(8L)).thenReturn(1);
        assertThat(service.rollback(1L, 8L).getId()).isEqualTo(8L);
    }

    private KnowledgeDocument document(Long id, String key, int version, KnowledgeDocumentStatus status) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setDocumentKey(key);
        document.setVersionNo(version);
        document.setStatus(status);
        return document;
    }
}
