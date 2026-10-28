package com.camunda.pbj2.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PatientRegistrationWorker {

    private static final Logger log = LoggerFactory.getLogger(PatientRegistrationWorker.class);

    @JobWorker(type = "validate-patient-registration", autoComplete = true)
    public Map<String, Object> handleValidateRegistration(final ActivatedJob job) {
        log.info("Executing job worker 'validate-patient-registration' for key: {}", job.getKey());
        Map<String, Object> variables = job.getVariablesAsMap();

        String patientRef = (String) variables.getOrDefault("patientReference", "PAT-UNKNOWN");
        String prefDept = (String) variables.getOrDefault("appointmentPreference", "GENERAL");
        Boolean simulateWorkerFailure = (Boolean) variables.getOrDefault("simulateWorkerFailure", false);

        // Lab 3 Scenario 5: Worker failure simulation
        if (Boolean.TRUE.equals(simulateWorkerFailure)) {
            log.error("Simulating worker crash on validate-patient-registration for patient: {}", patientRef);
            throw new RuntimeException("Simulated master patient index (MPI) lookup failure for: " + patientRef);
        }

        String normalizedDept = prefDept.trim().toUpperCase().replace(" ", "_");
        boolean valid = patientRef != null && !patientRef.isBlank();

        Map<String, Object> output = new HashMap<>();
        output.put("registrationValid", valid);
        output.put("normalizedDepartment", normalizedDept);
        output.put("urgencyScore", 50);
        output.put("validationNotes", "Patient demographics verified against national registry.");

        log.info("Registration validated: patient={}, dept={}, valid={}", patientRef, normalizedDept, valid);
        return output;
    }

    @JobWorker(type = "booking-confirmation-service", autoComplete = true)
    public Map<String, Object> handleBookingConfirmation(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String patientRef = (String) variables.getOrDefault("patientReference", "PAT-UNKNOWN");
        String slotId = (String) variables.getOrDefault("slotId", "SLOT-DEFAULT");
        log.info("Booking confirmation issued for patient: {}, slot: {}", patientRef, slotId);

        Map<String, Object> output = new HashMap<>();
        output.put("bookingConfirmationCode", "CONF-" + System.currentTimeMillis());
        output.put("bookingStatus", "CONFIRMED");
        return output;
    }
}
