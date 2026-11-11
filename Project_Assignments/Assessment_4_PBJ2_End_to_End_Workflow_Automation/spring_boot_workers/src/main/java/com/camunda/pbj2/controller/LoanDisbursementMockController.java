package com.camunda.pbj2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/loan")
public class LoanDisbursementMockController {

    private static final Logger log = LoggerFactory.getLogger(LoanDisbursementMockController.class);

    @PostMapping("/disburse")
    public ResponseEntity<Map<String, Object>> disburseLoan(@RequestBody Map<String, Object> request) {
        String applicantId = (String) request.getOrDefault("applicantId", "APP-UNKNOWN");
        Object amount = request.getOrDefault("amount", 0);
        log.info("Disbursing loan via REST Outbound Connector for applicant: {}, amount: ${}", applicantId, amount);

        Map<String, Object> response = new HashMap<>();
        response.put("applicantId", applicantId);
        response.put("disbursementStatus", "COMPLETED");
        response.put("disbursementRef", "DISB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
}
