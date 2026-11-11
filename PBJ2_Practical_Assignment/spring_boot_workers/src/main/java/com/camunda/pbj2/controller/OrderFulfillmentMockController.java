package com.camunda.pbj2.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/order")
public class OrderFulfillmentMockController {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfillmentMockController.class);

    @PostMapping("/fulfill")
    public ResponseEntity<Map<String, Object>> fulfillOrder(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.getOrDefault("orderId", "ORD-UNKNOWN");
        log.info("REST Outbound Connector order fulfillment dispatched for order: {}", orderId);

        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("fulfillmentStatus", "DISPATCHED");
        response.put("trackingNumber", "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return ResponseEntity.ok(response);
    }
}
