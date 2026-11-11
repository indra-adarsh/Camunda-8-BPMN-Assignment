package com.camunda.pbj2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationMockController {

    private static final Logger log = LoggerFactory.getLogger(NotificationMockController.class);

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNotification(@RequestBody Map<String, Object> request) {
        log.info("REST Outbound Connector notification dispatched: {}", request);

        Map<String, Object> response = new HashMap<>();
        response.put("notificationSent", true);
        response.put("sentTimestamp", System.currentTimeMillis());
        response.put("status", "DELIVERED");
        return ResponseEntity.ok(response);
    }
}
