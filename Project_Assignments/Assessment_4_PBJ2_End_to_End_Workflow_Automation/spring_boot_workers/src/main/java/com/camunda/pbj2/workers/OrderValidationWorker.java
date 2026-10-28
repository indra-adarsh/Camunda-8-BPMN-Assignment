package com.camunda.pbj2.workers;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OrderValidationWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderValidationWorker.class);

    @JobWorker(type = "validate-order-inventory", autoComplete = true)
    public Map<String, Object> handleValidateOrder(final ActivatedJob job) {
        log.info("Executing job worker 'validate-order-inventory' for key: {}", job.getKey());
        Map<String, Object> variables = job.getVariablesAsMap();

        String orderId = (String) variables.getOrDefault("orderId", "ORD-UNKNOWN");
        String productId = (String) variables.getOrDefault("productId", "PROD-DEFAULT");
        Number quantityNum = (Number) variables.getOrDefault("quantity", 1);
        Number unitPriceNum = (Number) variables.getOrDefault("unitPrice", 0.0);
        Boolean simulateWorkerFailure = (Boolean) variables.getOrDefault("simulateWorkerFailure", false);

        // Lab 2 Scenario 5: Worker failure simulation
        if (Boolean.TRUE.equals(simulateWorkerFailure)) {
            log.error("Simulating worker crash on validate-order-inventory for order: {}", orderId);
            throw new RuntimeException("Simulated inventory warehouse sync failure for order: " + orderId);
        }

        int quantity = quantityNum.intValue();
        double unitPrice = unitPriceNum.doubleValue();
        double orderTotal = quantity * unitPrice;

        // Check mock stock
        boolean outOfStock = productId.toUpperCase().contains("OUT-OF-STOCK");
        String stockStatus = outOfStock ? "OUT_OF_STOCK" : "IN_STOCK";
        int availableStock = outOfStock ? 0 : 150;

        Map<String, Object> output = new HashMap<>();
        output.put("orderValid", true);
        output.put("stockStatus", stockStatus);
        output.put("availableStock", availableStock);
        output.put("orderTotal", orderTotal);

        log.info("Order validated: orderId={}, total=${}, stockStatus={}", orderId, orderTotal, stockStatus);
        return output;
    }

    @JobWorker(type = "fulfillment-service", autoComplete = true)
    public Map<String, Object> handleFulfillment(final ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        String orderId = (String) variables.getOrDefault("orderId", "ORD-UNKNOWN");
        log.info("Fulfillment service initiated pick-pack-ship for order: {}", orderId);

        Map<String, Object> output = new HashMap<>();
        output.put("trackingNumber", "TRK-" + System.currentTimeMillis());
        output.put("fulfillmentStatus", "DISPATCHED");
        return output;
    }
}
