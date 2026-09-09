package com.liang.medical.knowledge.service;

import com.liang.medical.common.BusinessException;
import com.liang.medical.audit.service.AuditService;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.knowledge.entity.KnowledgeDocument;
import com.liang.medical.knowledge.KnowledgeDocumentStatus;
import com.liang.medical.knowledge.mapper.KnowledgeDocumentMapper;
import com.liang.medical.knowledge.service.KnowledgeDocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {
    static final long MAX_BYTES = 2L * 1024 * 1024;

    private final KnowledgeDocumentMapper documentMapper;
    private final AuditService auditService;

    public KnowledgeDocumentServiceImpl(KnowledgeDocumentMapper documentMapper) {
        this(documentMapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public KnowledgeDocumentServiceImpl(KnowledgeDocumentMapper documentMapper, AuditService auditService) {
        this.documentMapper = documentMapper;
        this.auditService = auditService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocument upload(Long adminId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("知识文档不能为空");
        }
        if (file.getSize() > MAX_BYTES) {
            throw badRequest("知识文档不能超过2MiB");
        }
        String documentName = sanitizeName(file.getOriginalFilename());
        if (!isTextDocument(documentName, file.getContentType())) {
            throw badRequest("仅支持 UTF-8 文本或 Markdown 文件");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "知识文档读取失败");
        }
        String content = decodeUtf8(bytes);
        String sha256 = sha256(bytes);
        KnowledgeDocument existing = documentMapper.findByContentSha256(sha256);
        if (existing != null) {
            return existing;
        }

        String documentKey = documentKey(documentName);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setDocumentKey(documentKey);
        document.setDocumentName(documentName);
        document.setContentType(normalizeContentType(file.getContentType()));
        document.setContentSha256(sha256);
        document.setContentText(content);
        document.setVersionNo(documentMapper.nextVersion(documentKey));
        document.setStatus(KnowledgeDocumentStatus.DRAFT);
        document.setCreatedBy(adminId);
        document.setCreatedAt(LocalDateTime.now());
        documentMapper.insert(document);
        audit(adminId, "KNOWLEDGE_UPLOAD", document.getId(), "SUCCESS", "version=" + document.getVersionNo());
        return document;
    }

    @Override
    public List<KnowledgeDocument> listVersions() {
        return documentMapper.findAllVersions();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocument publish(Long adminId, Long documentId) {
        KnowledgeDocument target = require(documentId);
        if (target.getStatus() == KnowledgeDocumentStatus.PUBLISHED) {
            return target;
        }
        if (target.getStatus() != KnowledgeDocumentStatus.DRAFT) {
            throw conflict("只有草稿版本可以发布");
        }
        documentMapper.archivePublished(target.getDocumentKey());
        if (documentMapper.markPublished(documentId) != 1) {
            throw conflict("知识版本已变更，请刷新后重试");
        }
        target.setStatus(KnowledgeDocumentStatus.PUBLISHED);
        target.setPublishedAt(LocalDateTime.now());
        audit(adminId, "KNOWLEDGE_PUBLISH", documentId, "SUCCESS", "version=" + target.getVersionNo());
        return target;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocument rollback(Long adminId, Long documentId) {
        KnowledgeDocument target = require(documentId);
        if (target.getStatus() != KnowledgeDocumentStatus.ARCHIVED) {
            throw conflict("只有历史版本可以回滚");
        }
        documentMapper.archivePublished(target.getDocumentKey());
        if (documentMapper.markPublished(documentId) != 1) {
            throw conflict("知识版本已变更，请刷新后重试");
        }
        target.setStatus(KnowledgeDocumentStatus.PUBLISHED);
        target.setPublishedAt(LocalDateTime.now());
        audit(adminId, "KNOWLEDGE_ROLLBACK", documentId, "SUCCESS", "version=" + target.getVersionNo());
        return target;
    }

    private KnowledgeDocument require(Long documentId) {
        KnowledgeDocument document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "知识文档不存在");
        }
        return document;
    }

    private String decodeUtf8(byte[] bytes) {
        try {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return decoded.toString();
        } catch (CharacterCodingException exception) {
            throw badRequest("仅支持 UTF-8 文本或 Markdown 文件");
        }
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private boolean isTextDocument(String name, String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String lowerName = name.toLowerCase(Locale.ROOT);
        return type.equals("text/plain") || type.equals("text/markdown") || type.equals("text/x-markdown")
                || lowerName.endsWith(".txt") || lowerName.endsWith(".md") || lowerName.endsWith(".markdown");
    }

    private String sanitizeName(String originalName) {
        String name = originalName == null ? "knowledge.txt" : originalName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        name = slash >= 0 ? name.substring(slash + 1) : name;
        name = name.trim();
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            throw badRequest("知识文档文件名无效");
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }

    private String documentKey(String name) {
        int dot = name.lastIndexOf('.');
        String key = dot > 0 ? name.substring(0, dot) : name;
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        return contentType == null || contentType.isBlank() ? "text/plain" : contentType.toLowerCase(Locale.ROOT);
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, 400, message);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, 409, message);
    }

    private void audit(Long adminId, String action, Long documentId, String result, String detail) {
        if (auditService != null) {
            auditService.record(new UserPrincipal(adminId, null, UserRole.ADMIN), action, "KNOWLEDGE_DOCUMENT",
                    documentId == null ? null : documentId.toString(), result, detail);
        }
    }
}
