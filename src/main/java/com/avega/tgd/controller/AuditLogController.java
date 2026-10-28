package com.avega.tgd.controller;

import com.avega.tgd.domain.entity.AuditLogEntity;
import com.avega.tgd.domain.enums.AuditEventType;
import com.avega.tgd.dto.response.ApiResponseDto;
import com.avega.tgd.dto.response.AuditLogResponseDto;
import com.avega.tgd.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for retrieving immutable system audit trail records.
 *
 * <p>Supports Requirement 5 ("Audit Logging"):
 * <ul>
 *   <li>Enables regulatory inspection of every ingestion, rule execution, and tax calculation event.</li>
 *   <li>Provides paginated queries filtered by transaction ID or event type.</li>
 *   <li>Provides complete chronological audit trails for specific transactions.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "Endpoints for retrieving system audit trails")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * Retrieves paginated audit logs with optional filtering by transaction ID or event type.
     *
     * @param transactionId optional transaction ID filter
     * @param eventType     optional event type filter (INGESTION, RULE_EXECUTION, TAX_COMPUTATION)
     * @param pageable      page request settings (defaults to 20 items sorted by timestamp)
     * @return HTTP 200 OK with page of {@link AuditLogResponseDto}
     */
    @GetMapping
    @Operation(summary = "Get audit logs with optional filtering by transactionId or eventType")
    public ResponseEntity<ApiResponseDto<Page<AuditLogResponseDto>>> getAuditLogs(
            @Parameter(description = "Filter by transaction ID")
            @RequestParam(required = false) String transactionId,

            @Parameter(description = "Filter by event type (INGESTION, RULE_EXECUTION, TAX_COMPUTATION)")
            @RequestParam(required = false) AuditEventType eventType,

            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {

        Page<AuditLogEntity> page = auditLogService.getAuditLogs(transactionId, eventType, pageable);
        Page<AuditLogResponseDto> dtoPage = page.map(AuditLogResponseDto::fromEntity);
        return ResponseEntity.ok(ApiResponseDto.ok(dtoPage));
    }

    /**
     * Retrieves all audit trail events recorded for a specific transaction.
     *
     * @param transactionId the unique transaction identifier
     * @return HTTP 200 OK with list of {@link AuditLogResponseDto} entries
     */
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
