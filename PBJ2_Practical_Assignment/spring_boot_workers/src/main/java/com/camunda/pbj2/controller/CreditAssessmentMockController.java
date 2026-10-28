package com.camunda.pbj2.controller;

import com.camunda.pbj2.dto.CreditAssessmentRequest;
import com.camunda.pbj2.dto.CreditAssessmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/credit")
public class CreditAssessmentMockController {

    private static final Logger log = LoggerFactory.getLogger(CreditAssessmentMockController.class);

    @PostMapping("/assess")
    public ResponseEntity<?> assessCredit(@RequestBody CreditAssessmentRequest request,
                                          @RequestParam(value = "simulateTimeout", defaultValue = "false") boolean simulateTimeout) {
        log.info("Received Credit Assessment request for applicant: {}", request.getApplicantId());

        // Lab 1 Scenario 4: Simulate API timeout or server failure
        if (simulateTimeout || (request.getApplicantId() != null && request.getApplicantId().contains("TIMEOUT"))) {
            log.warn("Simulating HTTP 504 Gateway Timeout for applicant: {}", request.getApplicantId());
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body("Downstream credit bureau did not respond within configured 5000ms SLA");
        }

        // Realistic synthetic credit score logic based on requested amount and income
        long creditScore = 780;
        int riskScore = 12;
        String remarks = "Excellent credit profile. Zero delinquent payments in 36 months.";

        if (request.getApplicantId() != null && request.getApplicantId().contains("SUBPRIME")) {
            creditScore = 560;
            riskScore = 85;
            remarks = "Subprime credit history. Multiple charge-offs and recent defaults detected.";
        } else if (request.getApplicantId() != null && request.getApplicantId().contains("REVIEW")) {
            creditScore = 660;
            riskScore = 48;
            remarks = "Near-prime credit profile. Limited credit history requires manual asset verification.";
        } else if (request.getMonthlyIncome() != null && request.getRequestedAmount() != null) {
            double multiple = request.getRequestedAmount() / Math.max(1.0, request.getMonthlyIncome());
            if (multiple > 10) {
                creditScore = 680;
                riskScore = 45;
                remarks = "High loan-to-income multiplier requires discretionary underwriter sign-off.";
            }
        }

        String bureauRef = "BUR-2026-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CreditAssessmentResponse response = new CreditAssessmentResponse(
                request.getApplicantId(),
                creditScore,
                riskScore,
                remarks,
                bureauRef
        );

        log.info("Returning Credit Assessment: applicant={}, score={}, riskTier={}", 
                request.getApplicantId(), creditScore, riskScore);
        return ResponseEntity.ok(response);
    }
}
