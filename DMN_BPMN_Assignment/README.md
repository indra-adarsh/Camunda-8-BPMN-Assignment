# Camunda 8 BPMN & DMN Advanced Decision Orchestration

This module contains end-to-end business process models and decision tables implementing Business Rule Task evaluation, FEEL unary expressions, hit policies (`UNIQUE` and `COLLECT SUM`), Single Result / Single Entry mapping, and conditional routing via Exclusive Gateways (XOR).

---

## Table of Contents
1. [Exercise 1: Loan Application Risk Routing](#exercise-1-loan-application-risk-routing)
   - [Overview & Objectives](#overview--objectives)
   - [BPMN 2.0 Process Flow](#bpmn-20-process-flow)
   - [DMN Decision Table (evaluate-loan-risk)](#dmn-decision-table-evaluate-loan-risk)
   - [FEEL Gateway Routing Conditions](#feel-gateway-routing-conditions)
2. [Exercise 2: Multi-Item Order Discount & Fulfillment Orchestration](#exercise-2-multi-item-order-discount--fulfillment-orchestration)
   - [Overview & Objectives](#overview--objectives-1)
   - [BPMN 2.0 Process Flow](#bpmn-20-process-flow-1)
   - [DMN Decision Table (calculate-discounts)](#dmn-decision-table-calculate-discounts)
   - [Dynamic Routing & Verification](#dynamic-routing--verification)
3. [Repository Files](#repository-files)
4. [Automated Testing & Verification](#automated-testing--verification)

---

## Exercise 1: Loan Application Risk Routing

### Overview & Objectives
Model an end-to-end loan approval workflow where a **Business Rule Task** evaluates risk tier and manual review requirement using a DMN table, followed by an **Exclusive Gateway (XOR)** that routes the process accordingly.

### BPMN 2.0 Process Flow
- **File**: `01_Loan_Application_Risk_Routing.bpmn`
- **Process ID**: `Process_LoanRiskRouting`
- **Start Event**: Receives loan application payload with variables `applicantAge`, `creditScore`, and `loanAmount`.
- **Business Rule Task (`Evaluate Loan Risk`)**:
  - **Decision ID**: `evaluate-loan-risk`
  - **Result Variable**: `loanRiskDecision`
  - **Result Mapping**: Single Result mapped to process variables `riskTier` and `requiresManualReview`.
- **Exclusive Gateway (`Evaluate Risk Tier & Manual Review`)**:
  - **Path 1 (Auto-Approve)**: Routes to Service Task `Auto-Approve and Disburse` $ightarrow$ End Event `Loan Auto-Approved & Disbursed`.
  - **Path 2 (Underwriter Review)**: Routes to User Task `Underwriter Review` $ightarrow$ End Event `Underwriter Review Completed`.
  - **Path 3 (Auto-Reject)**: Routes to Service Task `Auto-Reject Notification` $ightarrow$ End Event `Loan Rejected`.

### DMN Decision Table (`evaluate-loan-risk`)
- **File**: `01_evaluate_loan_risk.dmn`
- **Hit Policy**: `UNIQUE` (`U`)
- **Inputs**:
  - `creditScore` (`number` / `integer`)
  - `loanAmount` (`number` / `double`)
- **Outputs**:
  - `riskTier` (`string`): `"LOW"`, `"MEDIUM"`, `"HIGH"`
  - `requiresManualReview` (`boolean`): `true`, `false`

| Rule # | Credit Score (FEEL Unary Test) | Loan Amount (FEEL Unary Test) | Output: `riskTier` | Output: `requiresManualReview` |
|:------:|:------------------------------:|:-----------------------------:|:------------------:|:------------------------------:|
| **1**  | `>= 750`                       | `<= 50000`                    | `"LOW"`            | `false`                        |
| **2**  | `>= 750`                       | `> 50000`                     | `"MEDIUM"`         | `true`                         |
| **3**  | `[600..749]`                   | `-` (any)                     | `"MEDIUM"`         | `true`                         |
| **4**  | `< 600`                        | `-` (any)                     | `"HIGH"`           | `false`                        |

### FEEL Gateway Routing Conditions
The outgoing sequence flows from the XOR gateway evaluate Camunda FEEL expressions:
- **Auto-Approve Flow**:
  ```feel
  = riskTier = "LOW"
  ```
- **Underwriter Review Flow**:
  ```feel
  = riskTier = "MEDIUM" or requiresManualReview = true
  ```
- **Auto-Reject Flow**:
  ```feel
  = riskTier = "HIGH" and requiresManualReview = false
  ```

---

## Exercise 2: Multi-Item Order Discount & Fulfillment Orchestration

### Overview & Objectives
Handle multi-hit DMN evaluation (**Collect Sum**: `C+`) inside a BPMN process and use the aggregated result for dynamic task routing based on final invoice thresholds.

### BPMN 2.0 Process Flow
- **File**: `02_Order_Discount_Fulfillment.bpmn`
- **Process ID**: `Process_OrderDiscountFulfillment`
- **Start Event**: Receives payload `customerTier` (`"REGULAR"`, `"PREMIUM"`), `cartValue` (`double`), and `promoCode` (`string`).
- **Business Rule Task (`Calculate Total Discount`)**:
  - **Decision ID**: `calculate-discounts`
  - **Result Variable**: `totalDiscount`
  - **Result Mapping**: Single Entry (`singleEntry` since `C+` collapses multi-rule matches into a single numeric sum).
- **Script / Calculation Task (`Calculate Final Amount`)**:
  - **FEEL Script**:
    ```feel
    = cartValue * (1 - (totalDiscount / 100))
    ```
  - **Result Variable**: `finalAmount`
- **Exclusive Gateway (`Is Final Amount >= 1000?`)**:
  - **If `finalAmount >= 1000`**: Routes to User Task `Manager Sign-off`.
  - **If `finalAmount < 1000`**: Proceeds directly.
- **Service Task (`Send Order to Warehouse`)**: Merges both branches and completes fulfillment.
- **End Event**: `Order Fulfilled & Sent to Warehouse`.

### DMN Decision Table (`calculate-discounts`)
- **File**: `02_calculate_discounts.dmn`
- **Hit Policy**: `COLLECT` with `aggregation="SUM"` (`C+`)
- **Inputs**:
  - `customerTier` (`string`)
  - `cartValue` (`number` / `double`)
  - `promoCode` (`string`)
- **Output**:
  - `discountPercent` (`number` / `integer`)

| Rule # | Customer Tier | Cart Value | Promo Code (FEEL) | Output: `discountPercent` | Description |
|:------:|:-------------:|:----------:|:-----------------:|:-------------------------:|:------------|
| **1**  | `"PREMIUM"`   | `-`        | `-`               | `10`                      | Premium tier customers receive 10% discount |
| **2**  | `-`           | `>= 500`   | `-`               | `5`                       | High-value carts (>= 500) receive 5% discount |
| **3**  | `-`           | `-`        | `"FESTIVE10", "SPECIAL10"` | `10`              | Promotional discount for valid promo codes |
| **4**  | `-`           | `-`        | `-`               | `0`                       | Default base rule |

### Dynamic Routing & Verification
**Sample Payload Evaluation**:
- Input: `customerTier = "PREMIUM"`, `cartValue = 600`, `promoCode = "FESTIVE10"`
- Multi-Hit Aggregation:
  - Rule 1 matches (`"PREMIUM"`): `+10`
  - Rule 2 matches (`600 >= 500`): `+5`
  - Rule 3 matches (`"FESTIVE10"`): `+10`
  - Rule 4 matches (Default): `+0`
  - **Total Aggregated Discount (`totalDiscount`)**: `10 + 5 + 10 + 0 = 25%`
- Invoice Calculation:
  $$	ext{finalAmount} = 600 	imes \left(1 - rac{25}{100}ight) = 600 	imes 0.75 = 450.0$$
- Routing Decision:
  - Since `finalAmount` ($450.0$) $< 1000$, the process takes the `Proceed directly` flow straight to `Send Order to Warehouse` without requiring `Manager Sign-off`.

---

## Repository Files

```
Camunda-8-BPMN-Assignment/
├── BPMN_Assignment/                              # Basic BPMN Scenarios
│   ├── 01_Employee_Leave_Approval.bpmn
│   ├── 02_Online_Purchase_Order.bpmn
│   └── 03_IT_Service_Request.bpmn
├── DMN_BPMN_Assignment/                          # Advanced DMN & BPMN Orchestration
│   ├── 01_Loan_Application_Risk_Routing.bpmn
│   ├── 01_evaluate_loan_risk.dmn
│   ├── 02_Order_Discount_Fulfillment.bpmn
│   ├── 02_calculate_discounts.dmn
│   └── README.md
├── tests/
│   └── test_dmn_and_bpmn.py                      # Automated Verification & Test Suite
└── README.md
```

---

## Automated Testing & Verification

The repository includes an automated Python test suite that validates:
1. XML schema compliance and BPMN 2.0 / DMN 1.3 well-formedness.
2. Business Rule Task mappings, called decision IDs, and result variables.
3. XOR Gateway FEEL conditional expressions.
4. Full decision table logic execution for both Unique and Collect Sum hit policies across normal and boundary inputs.

To execute the test suite:
```bash
python -m unittest discover -s tests -p "test_*.py" -v
```
