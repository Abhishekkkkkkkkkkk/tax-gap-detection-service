package com.avega.tgd.domain.enums;

/**
 * Compliance evaluation outcome based on tax gap calculation:
 * - COMPLIANT: |taxGap| <= 1
 * - UNDERPAID: taxGap > 1
 * - OVERPAID: taxGap < -1
 * - NON_COMPLIANT: reportedTax missing/null
 */
public enum ComplianceStatus {
    COMPLIANT,
    UNDERPAID,
    OVERPAID,
    NON_COMPLIANT
}
