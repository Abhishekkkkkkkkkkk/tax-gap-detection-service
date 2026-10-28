package com.avega.tgd.controller;

import com.avega.tgd.domain.entity.ExceptionEntity;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.dto.response.ApiResponseDto;
import com.avega.tgd.dto.response.ExceptionResponseDto;
import com.avega.tgd.service.ExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for querying and filtering tax compliance exceptions.
 */
@RestController
@RequestMapping("/api/v1/exceptions")
@Tag(name = "Exceptions", description = "Endpoints for retrieving and filtering compliance exception records")
public class ExceptionController {

    private final ExceptionService exceptionService;

    public ExceptionController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    @GetMapping
    @Operation(summary = "Get all exceptions with optional filters (customerId, severity, ruleName)")
    public ResponseEntity<ApiResponseDto<Page<ExceptionResponseDto>>> getExceptions(
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) String customerId,

            @Parameter(description = "Filter by severity level (HIGH, MEDIUM, LOW)")
            @RequestParam(required = false) Severity severity,

            @Parameter(description = "Filter by rule name")
            @RequestParam(required = false) String ruleName,

            @ParameterObject @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {

        Page<ExceptionEntity> page = exceptionService.getExceptions(customerId, severity, ruleName, pageable);
        Page<ExceptionResponseDto> dtoPage = page.map(ExceptionResponseDto::fromEntity);
        return ResponseEntity.ok(ApiResponseDto.ok(dtoPage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exception record by ID")
    public ResponseEntity<ApiResponseDto<ExceptionResponseDto>> getExceptionById(@PathVariable Long id) {
        ExceptionEntity entity = exceptionService.getExceptionById(id);
        return ResponseEntity.ok(ApiResponseDto.ok(ExceptionResponseDto.fromEntity(entity)));
    }
}
