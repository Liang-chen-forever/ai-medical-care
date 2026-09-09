package com.liang.medical.triage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("triage_evidence")
public class TriageEvidence {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long triageCaseId;
    private String documentId;
    private String chunkId;
    private String excerpt;
    private Double score;
    @TableField("rank_no")
    private Integer rankNo;
    private String knowledgeVersion;
}
