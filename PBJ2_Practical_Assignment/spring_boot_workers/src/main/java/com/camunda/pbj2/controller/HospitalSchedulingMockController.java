package com.camunda.pbj2.controller;

import com.camunda.pbj2.dto.SlotQueryRequest;
import com.camunda.pbj2.dto.SlotQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/hospital/scheduling")
public class HospitalSchedulingMockController {

    private static final Logger log = LoggerFactory.getLogger(HospitalSchedulingMockController.class);

    @PostMapping("/available-slots")
    public ResponseEntity<?> queryAvailableSlots(@RequestBody SlotQueryRequest request) {
        log.info("Received Slot Query for patient: {}, department: {}", 
                request.getPatientReference(), request.getDepartment());

        String ref = request.getPatientReference() != null ? request.getPatientReference() : "";
        String dept = request.getDepartment() != null ? request.getDepartment().toUpperCase() : "GENERAL";

        // Lab 3 Scenario 5: Simulate scheduling server outage (503 Service Unavailable)
        if (ref.contains("FAIL")) {
            log.warn("Simulating HTTP 503 Scheduling Service Outage for patient: {}", ref);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Mock hospital scheduling subsystem is temporarily offline for maintenance");
        }

        // Lab 3 Scenario 4: Simulate department overbooked / 0 slots available
        if (ref.contains("NOSLOT") || dept.contains("DERMATOLOGY")) {
            log.info("No slots available for department: {}", dept);
            SlotQueryResponse noSlotsResponse = new SlotQueryResponse(
                    dept,
                    false,
                    null,
                    null,
                    0
            );
            return ResponseEntity.ok(noSlotsResponse);
        }

        // Lab 3 Scenario 1 & 2: Available slot returned
        String slotId = "SLOT-" + dept.substring(0, Math.min(dept.length(), 5)) + "-" + (1000 + (int)(Math.random() * 9000));
        String slotTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        SlotQueryResponse response = new SlotQueryResponse(
                dept,
                true,
                slotId,
                slotTime,
                4
        );

        log.info("Available slot confirmed: slotId={}, time={}", slotId, slotTime);
        return ResponseEntity.ok(response);
    }
}
