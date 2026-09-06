package com.Liang.java.ai.langchain4j.audit;

import com.Liang.java.ai.langchain4j.auth.UserPrincipal;

public interface AuditService {
    void record(UserPrincipal actor, String action, String targetType, String targetId, String result, String detail);
}
