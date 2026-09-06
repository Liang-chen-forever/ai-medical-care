package com.Liang.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("encounter")
public class Encounter {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long appointmentId;
    private Long doctorId;
    private Long patientId;
    private String summary;
    private String followUpAdvice;
    private LocalDateTime completedAt;
}
