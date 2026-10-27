package com.avega.tgd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Tax Gap Detection & Compliance Validation Service
 *
 * Spring Boot application verifying accuracy of financial transactions,
 * computing expected tax and tax gaps, executing database-driven compliance rules,
 * generating exception records, maintaining audit trails, and generating reports.
 */
@SpringBootApplication
public class TaxGapDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxGapDetectionApplication.class, args);
    }
}
