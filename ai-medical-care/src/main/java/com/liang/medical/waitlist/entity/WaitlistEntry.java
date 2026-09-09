package com.liang.medical.waitlist.entity;

import com.liang.medical.triage.TriageRiskLevel;
import com.liang.medical.waitlist.entity.WaitlistStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Comparator;

@Data
@TableName("waitlist_entry")
public class WaitlistEntry {
    public static final Comparator<WaitlistEntry> PRIORITY_ORDER = Comparator
            .comparing(WaitlistEntry::getPriority, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(WaitlistEntry::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
            .thenComparing(WaitlistEntry::getId, Comparator.nullsLast(Long::compareTo));

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scheduleId;
    private Long patientId;
    private Long triageCaseId;
    private Long appointmentId;
    private Integer priority;
    private WaitlistStatus status;
    private LocalDateTime offerExpiresAt;
    private LocalDateTime offeredAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;

    public static int priorityFor(TriageRiskLevel riskLevel) {
        return switch (riskLevel == null ? TriageRiskLevel.UNKNOWN : riskLevel) {
            case EMERGENCY -> 0;
            case ROUTINE -> 10;
            case UNKNOWN -> 20;
        };
    }
}
