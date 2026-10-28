package com.avega.tgd.controller;

import com.avega.tgd.domain.entity.ExceptionEntity;
import com.avega.tgd.domain.enums.Severity;
import com.avega.tgd.dto.response.ApiResponseDto;
import com.avega.tgd.dto.response.ExceptionResponseDto;
import com.avega.tgd.service.ExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller managing tax compliance exception investigations.
 *
 * <p>Directly fulfills Requirement 4 ("Exception Management - APIs Required"):
 * <ul>
 *   <li><b>Get all exceptions:</b> Paginated list of all flagged compliance violations.</li>
 *   <li><b>Filtering:</b> Filter dynamically by {@code customerId}, {@code severity} (HIGH, MEDIUM, LOW),
 *       and {@code ruleName}.</li>
 *   <li><b>Individual Inspection:</b> Retrieve full exception details by surrogate primary key ID.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/exceptions")
@Tag(name = "Exceptions", description = "Endpoints for retrieving and filtering compliance exception records")
public class ExceptionController {

    private final ExceptionService exceptionService;

    public ExceptionController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    /**
     * Retrieves a paginated list of exceptions with optional multi-attribute filters.
     *
     * <p>Filters are combined conjunctively (AND). Unsupplied filter parameters are ignored.
     *
     * @param customerId optional customer identifier filter
     * @param severity   optional severity level filter (HIGH, MEDIUM, LOW)
     * @param ruleName   optional rule name filter
     * @param pageable   pagination configuration (defaults to 20 items sorted by timestamp descending)
     * @return HTTP 200 OK with a page of {@link ExceptionResponseDto} objects
     */
    @GetMapping
    @Operation(summary = "Get all exceptions with optional filters (customerId, severity, ruleName)")
    public ResponseEntity<ApiResponseDto<Page<ExceptionResponseDto>>> getExceptions(
            @Parameter(description = "Filter by customer ID")
            @RequestParam(required = false) String customerId,

            @Parameter(description = "Filter by severity level (HIGH, MEDIUM, LOW)")
            @RequestParam(required = false) Severity severity,

            @Parameter(description = "Filter by rule name")
            @RequestParam(required = false) String ruleName,

            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable) {

        Page<ExceptionEntity> page = exceptionService.getExceptions(customerId, severity, ruleName, pageable);
        Page<ExceptionResponseDto> dtoPage = page.map(ExceptionResponseDto::fromEntity);
        return ResponseEntity.ok(ApiResponseDto.ok(dtoPage));
    }

    /**
     * Retrieves an individual exception by its database primary key.
     *
     * @param id the exception database ID
     * @return HTTP 200 OK with {@link ExceptionResponseDto}, or HTTP 404 NOT FOUND
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get exception record by ID")
    public ResponseEntity<ApiResponseDto<ExceptionResponseDto>> getExceptionById(@PathVariable Long id) {
        ExceptionEntity entity = exceptionService.getExceptionById(id);
        return ResponseEntity.ok(ApiResponseDto.ok(ExceptionResponseDto.fromEntity(entity)));
    }
}
