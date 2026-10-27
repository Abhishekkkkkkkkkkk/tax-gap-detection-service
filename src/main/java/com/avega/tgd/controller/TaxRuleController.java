package com.avega.tgd.controller;

import com.avega.tgd.domain.entity.TaxRuleEntity;
import com.avega.tgd.dto.request.TaxRuleRequestDto;
import com.avega.tgd.dto.response.ApiResponseDto;
import com.avega.tgd.dto.response.TaxRuleResponseDto;
import com.avega.tgd.service.RuleEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller exposing endpoints for configuring, toggling, and viewing database-driven tax rules.
 */
@RestController
@RequestMapping("/api/v1/rules")
@Tag(name = "Tax Rules", description = "Endpoints for managing database-driven configurable compliance rules")
public class TaxRuleController {

    private final RuleEngineService ruleEngineService;

    public TaxRuleController(RuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }

    @GetMapping
    @Operation(summary = "List all configured tax rules")
    public ResponseEntity<ApiResponseDto<List<TaxRuleResponseDto>>> getAllRules() {
        List<TaxRuleResponseDto> rules = ruleEngineService.getAllRules().stream()
                .map(TaxRuleResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDto.ok(rules));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tax rule by ID")
    public ResponseEntity<ApiResponseDto<TaxRuleResponseDto>> getRuleById(@PathVariable Long id) {
        TaxRuleEntity entity = ruleEngineService.getRuleById(id);
        return ResponseEntity.ok(ApiResponseDto.ok(TaxRuleResponseDto.fromEntity(entity)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a new tax rule (Admin only)")
    public ResponseEntity<ApiResponseDto<TaxRuleResponseDto>> createRule(
            @Valid @RequestBody TaxRuleRequestDto request) {
        TaxRuleEntity created = ruleEngineService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Tax rule created successfully", TaxRuleResponseDto.fromEntity(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update an existing tax rule configuration (Admin only)")
    public ResponseEntity<ApiResponseDto<TaxRuleResponseDto>> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody TaxRuleRequestDto request) {
        TaxRuleEntity updated = ruleEngineService.updateRule(id, request);
        return ResponseEntity.ok(ApiResponseDto.ok("Tax rule updated successfully", TaxRuleResponseDto.fromEntity(updated)));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Enable or disable a tax rule (Admin only)")
    public ResponseEntity<ApiResponseDto<TaxRuleResponseDto>> toggleRule(@PathVariable Long id) {
        TaxRuleEntity toggled = ruleEngineService.toggleRule(id);
        return ResponseEntity.ok(ApiResponseDto.ok(
                "Tax rule status changed to " + (toggled.isEnabled() ? "ENABLED" : "DISABLED"),
                TaxRuleResponseDto.fromEntity(toggled)));
    }
}
