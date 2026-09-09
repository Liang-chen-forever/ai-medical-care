package com.liang.medical.audit.impl;

import com.liang.medical.audit.AuditService;
import com.liang.medical.audit.TraceIdFilter;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.entity.AuditLog;
import com.liang.medical.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditServiceImpl implements AuditService {
    private final AuditLogMapper auditLogMapper;

    public AuditServiceImpl(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public void record(UserPrincipal actor, String action, String targetType, String targetId,
                       String result, String detail) {
        AuditLog log = new AuditLog();
        if (actor != null) {
            log.setActorId(actor.userId());
            log.setActorRole(actor.role() == null ? null : actor.role().name());
        }
        log.setAction(limit(action, 64));
        log.setTargetType(limit(targetType, 64));
        log.setTargetId(limit(targetId, 64));
        log.setTraceId(resolveTraceId());
        log.setResult(limit(result, 16));
        log.setDetail(sanitize(detail));
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    private String resolveTraceId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            Object value = attributes.getRequest().getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
            if (value instanceof String traceId && !traceId.isBlank()) {
                return traceId;
            }
        }
        return UUID.randomUUID().toString();
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.replaceAll("(?i)(password|passwd|token|authorization|api[-_ ]?key|secret)\\s*[:=]\\s*[^,; ]+", "$1=[REDACTED]");
        return limit(clean, 500);
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
