package com.camunda.pbj2.controller;

import com.camunda.pbj2.dto.PaymentRequest;
import com.camunda.pbj2.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class PaymentGatewayMockController {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayMockController.class);

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequest request) {
        log.info("Received Payment request for order: {}, total: ${}", request.getOrderId(), request.getOrderTotal());

        String token = request.getPaymentToken() != null ? request.getPaymentToken() : "";

        // Lab 2 Scenario 5: Simulate payment gateway internal failure (500)
        if (token.contains("server_error_500") || (request.getOrderId() != null && request.getOrderId().contains("FAIL"))) {
            log.warn("Simulating HTTP 500 Payment Gateway Server Error for order: {}", request.getOrderId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Payment Gateway Communication Error: Upstream processor failure (HTTP 500)");
        }

        // Lab 2 Scenario 3: Simulate payment decline
        if (token.contains("declined")) {
            log.info("Payment DECLINED for order: {}", request.getOrderId());
            PaymentResponse declinedResponse = new PaymentResponse(
                    request.getOrderId(),
                    "DECLINED",
                    null,
                    "Card declined: Insufficient funds in synthetic bank account",
                    0.0
            );
            return ResponseEntity.ok(declinedResponse);
        }

        // Lab 2 Scenario 1 & 4: Successful capture
        String txnId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "-CONFIRMED";
        PaymentResponse successResponse = new PaymentResponse(
                request.getOrderId(),
                "SUCCESS",
                txnId,
                "Payment captured successfully via synthetic clearing house",
                request.getOrderTotal()
        );

        log.info("Payment SUCCESSFUL for order: {}, txn: {}", request.getOrderId(), txnId);
        return ResponseEntity.ok(successResponse);
    }
}
