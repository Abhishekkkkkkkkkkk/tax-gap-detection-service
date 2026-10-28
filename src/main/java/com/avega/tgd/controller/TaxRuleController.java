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
 * REST controller for managing database-driven tax compliance rules and configurations.
 *
 * <p>Supports Requirement 3 ("Configurable Rule Engine Stored in Database"):
 * <ul>
 *   <li><b>Dynamic Lifecycle:</b> Rules can be inspected, created, modified, and toggled on/off.</li>
 *   <li><b>Fine-Grained RBAC:</b>
 *     <ul>
 *       <li>Read endpoints ({@code GET}) are accessible to both {@code ROLE_AUDITOR} and {@code ROLE_ADMIN}.</li>
 *       <li>Mutating endpoints ({@code POST}, {@code PUT}) are guarded by
 *           {@code @PreAuthorize("hasAuthority('ROLE_ADMIN')")}, ensuring only administrators
 *           can alter compliance policy parameters.</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/rules")
@Tag(name = "Tax Rules", description = "Endpoints for managing database-driven configurable compliance rules")
public class TaxRuleController {

    private final RuleEngineService ruleEngineService;

    public TaxRuleController(RuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }

    /**
     * Lists all registered tax compliance rules with their current enabled status and configurations.
     *
     * @return HTTP 200 OK with list of {@link TaxRuleResponseDto}
     */
    @GetMapping
    @Operation(summary = "List all configured tax rules")
    public ResponseEntity<ApiResponseDto<List<TaxRuleResponseDto>>> getAllRules() {
        List<TaxRuleResponseDto> rules = ruleEngineService.getAllRules().stream()
                .map(TaxRuleResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseDto.ok(rules));
    }

    /**
     * Retrieves an individual rule configuration by database ID.
     *
     * @param id the rule primary key
     * @return HTTP 200 OK with {@link TaxRuleResponseDto}, or HTTP 404 NOT FOUND
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get tax rule by ID")
    public ResponseEntity<ApiResponseDto<TaxRuleResponseDto>> getRuleById(@PathVariable Long id) {
        TaxRuleEntity entity = ruleEngineService.getRuleById(id);
        return ResponseEntity.ok(ApiResponseDto.ok(TaxRuleResponseDto.fromEntity(entity)));
    }

    /**
     * Creates a new tax compliance rule. Restricted to administrators.
     *
     * @param request valid rule creation request payload
     * @return HTTP 201 CREATED with the newly created {@link TaxRuleResponseDto}
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Create a new tax rule (Admin only)")
    public ResponseEntity<ApiResponseDto<TaxRuleResponseDto>> createRule(
            @Valid @RequestBody TaxRuleRequestDto request) {
        TaxRuleEntity created = ruleEngineService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok("Tax rule created successfully", TaxRuleResponseDto.fromEntity(created)));
    }

    /**
     * Updates an existing rule's description, severity, enabled state, or JSON threshold configuration.
     * Restricted to administrators.
     *
     * @param id      the rule ID to update
     * @param request updated rule parameters
     * @return HTTP 200 OK with updated {@link TaxRuleResponseDto}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update an existing tax rule configuration (Admin only)")
    public ResponseEntity<ApiResponseDto<TaxRuleResponseDto>> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody TaxRuleRequestDto request) {
        TaxRuleEntity updated = ruleEngineService.updateRule(id, request);
        return ResponseEntity.ok(ApiResponseDto.ok("Tax rule updated successfully", TaxRuleResponseDto.fromEntity(updated)));
    }

    /**
     * Toggles an existing rule between ENABLED and DISABLED. Restricted to administrators.
     *
     * @param id the rule ID to toggle
     * @return HTTP 200 OK with the updated rule entity reflecting the new enabled flag
     */
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
