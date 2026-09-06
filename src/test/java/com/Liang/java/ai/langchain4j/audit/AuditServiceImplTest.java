package com.Liang.java.ai.langchain4j.audit;

import com.Liang.java.ai.langchain4j.audit.impl.AuditServiceImpl;
import com.Liang.java.ai.langchain4j.auth.UserPrincipal;
import com.Liang.java.ai.langchain4j.auth.UserRole;
import com.Liang.java.ai.langchain4j.entity.AuditLog;
import com.Liang.java.ai.langchain4j.mapper.AuditLogMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;

class AuditServiceImplTest {
    @Test
    void redactsCredentialsAndBoundsAuditDetail() {
        AuditLogMapper mapper = mock(AuditLogMapper.class);
        AuditService service = new AuditServiceImpl(mapper);

        service.record(new UserPrincipal(7L, "alice", UserRole.PATIENT), "APPOINTMENT_BOOK",
                "APPOINTMENT", "55", "SUCCESS",
                "password=secret token=jwt-value api_key=top-secret ordinary detail");

        var captor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        verify(mapper).insert(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getActorId()).isEqualTo(7L);
        assertThat(saved.getActorRole()).isEqualTo("PATIENT");
        assertThat(saved.getDetail()).doesNotContain("secret", "jwt-value", "top-secret")
                .contains("[REDACTED]");
        assertThat(saved.getTraceId()).isNotBlank();
    }
}
