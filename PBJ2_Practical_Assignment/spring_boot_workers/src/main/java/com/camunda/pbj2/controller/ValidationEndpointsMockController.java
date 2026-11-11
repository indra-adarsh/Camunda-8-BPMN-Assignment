package com.camunda.pbj2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock REST endpoints for Process Validations (Lab 1, Lab 2, Lab 3)
 * Allows automated service tasks to execute via Camunda 8 REST Outbound Connector.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ValidationEndpointsMockController {

    private static final Logger log = LoggerFactory.getLogger(ValidationEndpointsMockController.class);

    /**
     * Lab 1: Applicant Validation Endpoint
     * POST /api/applicant/validate
     */
    @PostMapping("/applicant/validate")
    public ResponseEntity<Map<String, Object>> validateApplicant(@RequestBody Map<String, Object> request) {
        log.info("[REST Connector] Received applicant validation request: {}", request);

        String applicantId = (String) request.getOrDefault("applicantId", "APP-UNKNOWN");
        String applicantName = (String) request.getOrDefault("applicantName", "Anonymous");
        Number requestedAmountNum = (Number) request.getOrDefault("requestedAmount", 0);
        Number monthlyIncomeNum = (Number) request.getOrDefault("monthlyIncome", 0);
        Number existingMonthlyDebtNum = (Number) request.getOrDefault("existingMonthlyDebt", 0);
        Boolean simulateWorkerFailure = (Boolean) request.getOrDefault("simulateWorkerFailure", false);

        if (Boolean.TRUE.equals(simulateWorkerFailure) || applicantId.contains("FAIL-RECOVER")) {
            log.error("CRITICAL: Inducing simulated incident for applicant: {}", applicantId);
            throw new RuntimeException("Simulated validation service failure on applicant: " + applicantId);
        }

        double requestedAmount = requestedAmountNum.doubleValue();
        double monthlyIncome = monthlyIncomeNum.doubleValue();
        double existingDebt = existingMonthlyDebtNum.doubleValue();

        boolean applicantValid = true;
        String notes = "Applicant details successfully validated.";

        if (monthlyIncome <= 0 || requestedAmount <= 0) {
            applicantValid = false;
            notes = "Validation failed: Monthly income and requested loan amount must be strictly positive.";
        }

        double dti = monthlyIncome > 0 ? (existingDebt / monthlyIncome) * 100.0 : 100.0;
        dti = Math.round(dti * 10.0) / 10.0;

        Map<String, Object> response = new HashMap<>();
        response.put("applicantValid", applicantValid);
        response.put("debtToIncomeRatio", dti);
        response.put("validationNotes", notes);

        log.info("[REST Connector] Applicant validation outcome: {}", response);
        return ResponseEntity.ok(response);
    }

    /**
     * Lab 2: Order & Inventory Validation Endpoint
     * POST /api/order/validate
     */
    @PostMapping("/order/validate")
    public ResponseEntity<Map<String, Object>> validateOrder(@RequestBody Map<String, Object> request) {
        log.info("[REST Connector] Received order validation request: {}", request);

        String orderId = (String) request.getOrDefault("orderId", "ORD-UNKNOWN");
        String productId = (String) request.getOrDefault("productId", "PROD-DEFAULT");
        Number quantityNum = (Number) request.getOrDefault("quantity", 1);
        Number unitPriceNum = (Number) request.getOrDefault("unitPrice", 0.0);
        Boolean simulateWorkerFailure = (Boolean) request.getOrDefault("simulateWorkerFailure", false);

        if (Boolean.TRUE.equals(simulateWorkerFailure)) {
            log.error("CRITICAL: Simulating inventory sync failure for order: {}", orderId);
            throw new RuntimeException("Simulated inventory warehouse sync failure for order: " + orderId);
        }

        int quantity = quantityNum.intValue();
        double unitPrice = unitPriceNum.doubleValue();
        double orderTotal = quantity * unitPrice;

        boolean outOfStock = productId.toUpperCase().contains("OUT-OF-STOCK");
        String stockStatus = outOfStock ? "OUT_OF_STOCK" : "IN_STOCK";
        int availableStock = outOfStock ? 0 : 150;

        Map<String, Object> response = new HashMap<>();
        response.put("orderValid", true);
        response.put("stockStatus", stockStatus);
        response.put("availableStock", availableStock);
        response.put("orderTotal", orderTotal);

        log.info("[REST Connector] Order validation outcome: {}", response);
        return ResponseEntity.ok(response);
    }

    /**
     * Lab 3: Patient Registration Validation Endpoint
     * POST /api/patient/validate
     */
    @PostMapping("/patient/validate")
    public ResponseEntity<Map<String, Object>> validatePatient(@RequestBody Map<String, Object> request) {
        log.info("[REST Connector] Received patient registration validation request: {}", request);

        String patientRef = (String) request.getOrDefault("patientReference", "PAT-UNKNOWN");
        String prefDept = (String) request.getOrDefault("appointmentPreference", "GENERAL");
        Boolean simulateWorkerFailure = (Boolean) request.getOrDefault("simulateWorkerFailure", false);

        if (Boolean.TRUE.equals(simulateWorkerFailure)) {
            log.error("CRITICAL: Simulating MPI lookup failure for patient: {}", patientRef);
            throw new RuntimeException("Simulated master patient index (MPI) lookup failure for: " + patientRef);
        }

        String normalizedDept = prefDept.trim().toUpperCase().replace(" ", "_");
        boolean valid = patientRef != null && !patientRef.isBlank();

        Map<String, Object> response = new HashMap<>();
        response.put("registrationValid", valid);
        response.put("normalizedDepartment", normalizedDept);
        response.put("urgencyScore", 50);
        response.put("validationNotes", "Patient demographics verified against national registry.");

        log.info("[REST Connector] Patient validation outcome: {}", response);
        return ResponseEntity.ok(response);
    }
}
