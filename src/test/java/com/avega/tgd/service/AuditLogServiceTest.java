package com.avega.tgd.service;

import com.avega.tgd.domain.entity.AuditLogEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import com.avega.tgd.repository.AuditLogRepository;
import com.avega.tgd.service.impl.AuditLogServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Log Service Unit Tests")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl(auditLogRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("logEvent should serialize payload and persist audit entity")
    void testLogEventPersistsPayload() {
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> payload = Map.of("status", "SUCCESS", "taxGap", 0.0);
        AuditLogEntity entity = auditLogService.logEvent(AuditEventType.TAX_COMPUTATION, "TX-100", payload);

        assertNotNull(entity);
        assertEquals(AuditEventType.TAX_COMPUTATION, entity.getEventType());
        assertEquals("TX-100", entity.getTransactionId());
        assertTrue(entity.getDetailJson().contains("\"status\":\"SUCCESS\""));
        verify(auditLogRepository).save(any(AuditLogEntity.class));
    }
}
