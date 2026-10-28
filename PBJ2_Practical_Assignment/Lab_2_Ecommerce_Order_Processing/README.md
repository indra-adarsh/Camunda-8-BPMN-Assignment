# Lab 2: End-to-End E-Commerce Order Processing and Fulfilment

## 1. Problem Statement & Architecture
The objective of Lab 2 is to architect and execute a resilient order-to-cash workflow in **Camunda 8**. The process coordinates:
- **Order Ingestion & Inventory Validation**: Synchronously checking product inventory availability via an external Spring Boot Zeebe job worker.
- **Dynamic Business Rules**: Determining fulfillment priority, manual review necessity, and target dispatch SLAs based on order total, customer loyalty tiers, and shipping preferences using DMN.
- **Human Verification User Task**: Providing an auditable Tasklist screen for high-value transactions (`orderTotal > $5,000`).
- **REST Outbound Payment Processing**: Communicating with an external mock payment gateway via Camunda 8 HTTP REST Outbound Connector.
- **Fail-Safe Gateways & Incident Handling**: Halting orders gracefully on stock shortages or payment declines, while enabling zero-data-loss incident retries in Operate on infrastructure crashes.

```
[Start: Order Placed]
          │
          ▼
[Worker: Validate & Check Stock]
          │
          ▼
    [Stock Available?] ──(No)──► [Notify Out of Stock] ──► [End: Cancelled]
          │ (Yes)
          ▼
[DMN: Determine Order Priority]
          │
          ▼
  [Requires Review?] ──(Yes)──► [Tasklist: Order Verification]
          │ (No)                              │
          │                   ┌───────────────┴───────────────┐
          │               (Approved)                      (Rejected)
          │                   │                               │
          │                   │                               ▼
          │                   │                   [Notify Order Cancellation]
          │                   │                               │
          │                   │                               ▼
          │                   │                       [End: Review Rejected]
          ▼                   ▼
[REST Connector: Process Payment Gateway]
          │
          ▼
    [Payment Success?] ──(Declined)──► [Notify Declined] ──► [End: Cancelled]
          │ (Success)
          ▼
[Fulfill & Dispatch Order]
          │
          ▼
[End: Order Fulfilled Successfully]
```

---

## 2. BPMN Model & Workflow Specification

- **BPMN File**: `models/lab2_order_processing_fulfillment.bpmn`
- **Process ID**: `Process_EcommerceOrderProcessing`
- **Execution Platform**: Camunda Cloud 8.5+

### Workflow Activities & Task Types
1. **`Event_OrderReceived` (Start Event)**: Ingests synthetic customer orders containing `orderId`, `customerReference`, `productId`, `quantity`, `unitPrice`, `deliveryPreference`, and `paymentToken`.
2. **`Activity_ValidateAndCheckInventory` (Service Task)**:
   - **Job Type**: `validate-order-inventory`
   - **Worker Logic**: Computes `orderTotal = quantity * unitPrice`, evaluates synthetic stock database. Sets `stockStatus = "IN_STOCK"` or `"OUT_OF_STOCK"`.
3. **`Gateway_StockCheck` (Exclusive Gateway)**:
   - Condition: `= stockStatus = "OUT_OF_STOCK"` -> routes to `Activity_NotifyOutOfStock` -> terminates at `Event_End_OutOfStock`.
   - Default: Routes to `Activity_DeterminePriorityDMN`.
4. **`Activity_DeterminePriorityDMN` (Business Rule Task)**:
   - **Called Decision ID**: `decision_order_priority`
   - **Result Variable**: `priorityResult`
   - Maps `orderPriority`, `manualReviewRequired`, `fulfillmentSlaHours`, `priorityReason`.
5. **`Gateway_ManualReviewCheck` (Exclusive Gateway)**:
   - Condition: `= manualReviewRequired = true or orderTotal > 5000` -> routes to `Activity_ManualOrderVerification`.
   - Default: Bypasses manual review directly to payment.
6. **`Activity_ManualOrderVerification` (User Task in Tasklist)**:
   - **Candidate Group**: `order-managers`
   - Human operator reviews order total, VIP priority reason, and inputs `reviewApproved` (boolean) and `reviewRemarks`.
7. **`Gateway_ReviewOutcome` (Exclusive Gateway)**:
   - Condition: `= reviewApproved = true` -> proceeds to payment.
   - Default: Routes to `Activity_NotifyOrderRejected` -> terminates at `Event_End_ReviewRejected`.
8. **`Activity_ProcessPaymentREST` (Service Task / REST Connector)**:
   - **Job Type**: `io.camunda:http-json:1`
   - **Method**: `POST`
   - **Target Endpoint**: `http://localhost:8080/api/payment/process`
   - **Payload**: `={orderId: orderId, orderTotal: orderTotal, paymentToken: paymentToken}`
   - **Outputs**: Maps `paymentStatus` ("SUCCESS", "DECLINED", "ERROR"), `transactionId`, `paymentMessage`.
9. **`Gateway_PaymentOutcome` (Exclusive Gateway)**:
   - Condition: `= paymentStatus = "DECLINED"` -> `Activity_NotifyPaymentDeclined`.
   - Default: Routes to `Activity_FulfillAndDispatch`.
10. **`Activity_FulfillAndDispatch` (Service Task)**:
    - Generates warehouse pick-pack-ship manifest and completes the order.

---

## 3. DMN Decision Table Specification

- **DMN File**: `models/lab2_order_priority_rules.dmn`
- **Decision ID**: `decision_order_priority`
- **Hit Policy**: `UNIQUE`

### Input & Output Definitions
| Parameter | TypeRef | Category | Description |
| :--- | :--- | :--- | :--- |
| `orderTotal` | `number` | Input | Calculated monetary value of order ($) |
| `customerCategory` | `string` | Input | Loyalty status: `"PLATINUM"`, `"GOLD"`, `"REGULAR"` |
| `deliveryPreference` | `string` | Input | Requested speed: `"OVERNIGHT"`, `"EXPRESS"`, `"STANDARD"` |
| `priority` | `string` | Output | Fulfillment tier: `"HIGH"`, `"MEDIUM"`, `"LOW"` |
| `manualReviewRequired` | `boolean` | Output | Audit flag triggering human verification |
| `fulfillmentSlaHours` | `number` | Output | Warehouse dispatch SLA threshold (hours) |
| `priorityReason` | `string` | Output | Documented business explanation |

### Decision Rules Table
| Rule # | Order Total ($) | Customer Tier | Delivery Pref | Priority | Review? | SLA (Hrs) | Rationale |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **1** | `> 5000` | `-` | `-` | `"HIGH"` | `true` | `4` | High-value order requires fraud and manual clearance review |
| **2** | `<= 5000` | `"PLATINUM"` | `-` | `"HIGH"` | `false` | `6` | Platinum VIP customer guaranteed expedited dispatch |
| **3** | `<= 5000` | `"GOLD"` | `"OVERNIGHT"` | `"HIGH"` | `false` | `8` | Gold tier overnight priority dispatch |
| **4** | `<= 5000` | `"GOLD"` | `not("OVERNIGHT")`| `"MEDIUM"`| `false` | `24` | Gold tier standard priority fulfillment |
| **5** | `<= 5000` | `"REGULAR"` | `"OVERNIGHT"` | `"MEDIUM"`| `false` | `12` | Regular customer expedited overnight queue |
| **6** | `<= 5000` | `"REGULAR"` | `"EXPRESS"` | `"MEDIUM"`| `false` | `24` | Regular customer express packaging queue |
| **7** | `<= 5000` | `"REGULAR"` | `"STANDARD"` | `"LOW"` | `false` | `48` | Regular customer economy fulfillment queue |

---

## 4. Process-Variable Dictionary

| Variable Name | Type | Scope | Source / Activity | Description |
| :--- | :--- | :--- | :--- | :--- |
| `orderId` | `String` | Process | Start Payload | Unique synthetic order identifier (e.g. `ORD-2026-101`) |
| `customerReference` | `String` | Process | Start Payload | Customer account code |
| `customerCategory` | `String` | Process | Start Payload | Loyalty category: `"PLATINUM"`, `"GOLD"`, `"REGULAR"` |
| `productId` | `String` | Process | Start Payload | Stock keeping unit (SKU) |
| `quantity` | `Integer` | Process | Start Payload | Number of items purchased |
| `unitPrice` | `Double` | Process | Start Payload | Unit cost per item |
| `deliveryPreference` | `String` | Process | Start Payload | Shipping tier selected by customer |
| `paymentToken` | `String` | Process | Start Payload | Synthetic mock payment token (e.g. `tok_visa_valid_9988`) |
| `orderValid` | `Boolean` | Process | `Activity_ValidateAndCheckInventory` | Validation flag |
| `orderTotal` | `Double` | Process | `Activity_ValidateAndCheckInventory` | Evaluated total: `quantity * unitPrice` |
| `stockStatus` | `String` | Process | `Activity_ValidateAndCheckInventory` | `"IN_STOCK"` or `"OUT_OF_STOCK"` |
| `availableStock` | `Integer` | Process | `Activity_ValidateAndCheckInventory` | Units remaining in warehouse inventory |
| `orderPriority` | `String` | Process | `Activity_DeterminePriorityDMN` | `"HIGH"`, `"MEDIUM"`, `"LOW"` |
| `manualReviewRequired` | `Boolean` | Process | `Activity_DeterminePriorityDMN` | True if manual security review is triggered |
| `fulfillmentSlaHours` | `Integer` | Process | `Activity_DeterminePriorityDMN` | Dispatch SLA |
| `priorityReason` | `String` | Process | `Activity_DeterminePriorityDMN` | Decision rationale |
| `reviewApproved` | `Boolean` | Process | `Activity_ManualOrderVerification` | Human reviewer verdict in Tasklist |
| `reviewRemarks` | `String` | Process | `Activity_ManualOrderVerification` | Auditor notes in Tasklist |
| `paymentStatus` | `String` | Process | `Activity_ProcessPaymentREST` | Gateway response (`"SUCCESS"`, `"DECLINED"`, `"ERROR"`) |
| `transactionId` | `String` | Process | `Activity_ProcessPaymentREST` | Synthetic payment transaction confirmation reference |

---

## 5. REST Request / Response Examples

### Endpoint: `POST /api/payment/process`
- **Request Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "orderId": "ORD-2026-101",
    "orderTotal": 2400.0,
    "paymentToken": "tok_visa_valid_9988"
  }
  ```
- **Successful Response (HTTP 200 OK)**:
  ```json
  {
    "orderId": "ORD-2026-101",
    "paymentStatus": "SUCCESS",
    "transactionId": "TXN-9988-CONFIRMED",
    "paymentMessage": "Payment captured successfully via synthetic clearing house",
    "settledAmount": 2400.0
  }
  ```
- **Declined Response (HTTP 200 OK)**:
  ```json
  {
    "orderId": "ORD-2026-303",
    "paymentStatus": "DECLINED",
    "transactionId": null,
    "paymentMessage": "Card declined: Insufficient funds in synthetic bank account",
    "settledAmount": 0.0
  }
  ```

---

## 6. Test-Case Matrix & Verification Evidence

| Scenario ID | Test Input Highlights | Expected Gateway Route | Actual Outcome | Status |
| :--- | :--- | :--- | :--- | :---: |
| **Scenario 1** | In-stock, Platinum VIP, Total $2,400 | `Gateway_StockCheck` (In Stock) -> `Gateway_PaymentOutcome` (Success) | Order Fulfilled Successfully | **PASS** |
| **Scenario 2** | `productId = "PROD-OUT-OF-STOCK"` | `Gateway_StockCheck` (Out of Stock) | Order Cancelled (Out of Stock) | **PASS** |
| **Scenario 3** | `paymentToken = "tok_declined_insufficient_funds"` | `Gateway_PaymentOutcome` (Declined) | Order Cancelled (Payment Declined) | **PASS** |
| **Scenario 4** | Total $7,500 (> $5,000 threshold) | `Gateway_ManualReviewCheck` -> Tasklist Review -> Approved | Manually cleared in Tasklist; Fulfilled | **PASS** |
| **Scenario 5** | `simulatePaymentError = true` (HTTP 500) | REST Connector Incident in Operate | HTTP 500 incident logged; token corrected; retried | **PASS** |

---

## 7. Operate Monitoring & Incident Investigation Guide

1. **Simulate Service Failure**:
   - Launch process instance with `paymentToken = "tok_simulate_server_error_500"`.
   - The mock payment endpoint returns HTTP 500 Internal Server Error.
2. **Inspect Incident in Operate**:
   - Locate instance in Operate.
   - Target task `Activity_ProcessPaymentREST` shows active incident: `Connector HTTP 500 Internal Server Error`.
3. **Correct Cause & Retry**:
   - In Operate variable panel, edit variable `paymentToken` to `"tok_visa_valid_9988"`.
   - Click **Retry** on the incident.
   - Payment succeeds, and token transitions to `Activity_FulfillAndDispatch`.
