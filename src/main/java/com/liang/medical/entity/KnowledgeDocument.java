package com.liang.medical.entity;

import com.liang.medical.knowledge.KnowledgeDocumentStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_document")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String documentKey;
    private String documentName;
    private String contentType;
    private String contentSha256;
    private String contentText;
    private Integer versionNo;
    private KnowledgeDocumentStatus status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
