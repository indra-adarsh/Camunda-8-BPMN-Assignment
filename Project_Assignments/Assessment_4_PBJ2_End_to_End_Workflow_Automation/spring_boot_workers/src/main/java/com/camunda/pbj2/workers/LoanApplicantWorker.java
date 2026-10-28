package com.camunda.pbj2.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoanApplicantWorker {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicantWorker.class);

    @JobWorker(type = "validate-loan-applicant", autoComplete = true)
    public Map<String, Object> handleValidateApplicant(final ActivatedJob job) {
        log.info("Executing job worker 'validate-loan-applicant' for key: {}", job.getKey());
        Map<String, Object> variables = job.getVariablesAsMap();

        String applicantId = (String) variables.getOrDefault("applicantId", "APP-UNKNOWN");
        String applicantName = (String) variables.getOrDefault("applicantName", "Anonymous");
        Number requestedAmountNum = (Number) variables.getOrDefault("requestedAmount", 0);
        Number monthlyIncomeNum = (Number) variables.getOrDefault("monthlyIncome", 0);
        Number existingMonthlyDebtNum = (Number) variables.getOrDefault("existingMonthlyDebt", 0);
        Boolean simulateWorkerFailure = (Boolean) variables.getOrDefault("simulateWorkerFailure", false);

        // Lab 1 Scenario 5: Intentionally induce worker crash for Operate incident investigation
        if (Boolean.TRUE.equals(simulateWorkerFailure) || applicantId.contains("FAIL-RECOVER")) {
            log.error("CRITICAL: Inducing simulated worker crash for Operate incident testing on applicant: {}", applicantId);
            throw new RuntimeException("Simulated worker database connectivity failure on applicant: " + applicantId);
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

        // Calculate Debt-to-Income (DTI) ratio
        double dti = monthlyIncome > 0 ? (existingDebt / monthlyIncome) * 100.0 : 100.0;
        // Round to 1 decimal place
        dti = Math.round(dti * 10.0) / 10.0;

        Map<String, Object> output = new HashMap<>();
        output.put("applicantValid", applicantValid);
        output.put("debtToIncomeRatio", dti);
        output.put("validationNotes", notes);

        log.info("Validation completed: applicant={}, valid={}, DTI={}%", applicantId, applicantValid, dti);
        return output;
    }

    @JobWorker(type = "loan-disbursement-service", autoComplete = true)
    public Map<String, Object> handleDisbursement(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String applicantId = (String) variables.getOrDefault("applicantId", "APP-UNKNOWN");
        Number amount = (Number) variables.getOrDefault("requestedAmount", 0);
        Number rate = (Number) variables.getOrDefault("interestRate", 0.0);

        log.info("Disbursing loan: applicant={}, amount=${}, rate={}%", applicantId, amount, rate);

        Map<String, Object> output = new HashMap<>();
        output.put("disbursementStatus", "COMPLETED");
        output.put("disbursementRef", "DISB-" + System.currentTimeMillis());
        return output;
    }

    @JobWorker(type = "notification-service", autoComplete = true)
    public Map<String, Object> handleNotification(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        log.info("Notification service dispatched notice for workflow instance: {}", job.getProcessInstanceKey());

        Map<String, Object> output = new HashMap<>();
        output.put("notificationSent", true);
        output.put("sentTimestamp", System.currentTimeMillis());
        return output;
    }
}
