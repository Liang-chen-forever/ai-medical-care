package com.liang.medical.audit.service;

import com.liang.medical.auth.UserPrincipal;

public interface AuditService {
    void record(UserPrincipal actor, String action, String targetType, String targetId, String result, String detail);
}
