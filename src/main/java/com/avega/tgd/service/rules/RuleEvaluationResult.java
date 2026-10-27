package com.avega.tgd.service.rules;

import com.avega.tgd.domain.enums.Severity;

/**
 * Result returned by a tax rule evaluation.
 */
public class RuleEvaluationResult {

    private final boolean passed;
    private final Severity severity;
    private final String message;

    private RuleEvaluationResult(boolean passed, Severity severity, String message) {
        this.passed = passed;
        this.severity = severity;
        this.message = message;
    }

    public static RuleEvaluationResult pass() {
        return new RuleEvaluationResult(true, null, "Rule passed");
    }

    public static RuleEvaluationResult fail(Severity severity, String message) {
        return new RuleEvaluationResult(false, severity, message);
    }

    public boolean isPassed() {
        return passed;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }
}
