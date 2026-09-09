package com.liang.medical.audit;

import com.liang.medical.audit.service.AuditService;
import com.liang.medical.audit.service.AuditServiceImpl;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.audit.entity.AuditLog;
import com.liang.medical.audit.mapper.AuditLogMapper;
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
