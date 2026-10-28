package com.avega.tgd.controller;

import com.avega.tgd.domain.entity.AuditLogEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import com.avega.tgd.dto.response.ApiResponseDto;
import com.avega.tgd.dto.response.AuditLogResponseDto;
import com.avega.tgd.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for retrieving system audit trail records.
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "Endpoints for retrieving system audit trails")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Get audit logs with optional filtering by transactionId or eventType")
    public ResponseEntity<ApiResponseDto<Page<AuditLogResponseDto>>> getAuditLogs(
            @Parameter(description = "Filter by transaction ID")
            @RequestParam(required = false) String transactionId,

            @Parameter(description = "Filter by event type (INGESTION, RULE_EXECUTION, TAX_COMPUTATION)")
            @RequestParam(required = false) AuditEventType eventType,

            @ParameterObject @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {

        Page<AuditLogEntity> page = auditLogService.getAuditLogs(transactionId, eventType, pageable);
        Page<AuditLogResponseDto> dtoPage = page.map(AuditLogResponseDto::fromEntity);
        return ResponseEntity.ok(ApiResponseDto.ok(dtoPage));
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get all audit logs for a specific transaction")
    public ResponseEntity<ApiResponseDto<List<AuditLogResponseDto>>> getByTransactionId(
            @PathVariable String transactionId) {
        List<AuditLogEntity> logs = auditLogService.getAuditLogsForTransaction(transactionId);
        List<AuditLogResponseDto> dtoList = logs.stream()
                .map(AuditLogResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDto.ok(dtoList));
    }
}
