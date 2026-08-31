# Camunda 8 BPMN & DMN Process Modeling Repository

This repository contains comprehensive BPMN 2.0 process models and DMN 1.3 decision tables created using Camunda 8 Modeler for enterprise business workflow automation.

---

## Overview of Contents

### 1. Advanced DMN & BPMN Orchestration ([DMN_BPMN_Assignment/](DMN_BPMN_Assignment/))
- **Exercise 1: Loan Application Risk Routing**
  - BPMN: `01_Loan_Application_Risk_Routing.bpmn`
  - DMN: `01_evaluate_loan_risk.dmn` (Hit Policy: `UNIQUE`, Single Result mapping)
  - Evaluates applicant risk tier and manual review flag with FEEL unary expressions, routing automatically to Auto-Approve, Underwriter Review, or Auto-Reject.
- **Exercise 2: Multi-Item Order Discount & Fulfillment Orchestration**
  - BPMN: `02_Order_Discount_Fulfillment.bpmn`
  - DMN: `02_calculate_discounts.dmn` (Hit Policy: `COLLECT SUM` `C+`, Single Entry mapping)
  - Aggregates multi-hit discount rules, computes final invoice amount via FEEL script task, and dynamically routes high-value orders ($>= 1000$) to Manager Sign-off.

### 2. Core BPMN Process Scenarios ([BPMN_Assignment/](BPMN_Assignment/))
- **01: Employee Leave Approval** (`01_Employee_Leave_Approval.bpmn`)
  - HR leave balance validation, manager review, and balance update.
- **02: Online Purchase Order Processing** (`02_Online_Purchase_Order.bpmn`)
  - Product stock check, payment processing, and shipment dispatch.
- **03: IT Service Request** (`03_IT_Service_Request.bpmn`)
  - Ticket severity assessment, tier-based technician assignment, and escalation.

---

## Automated Testing

Run the automated test suite to verify XML validity, FEEL expressions, and decision table evaluation:
```bash
python -m unittest discover -s tests -p "test_*.py" -v
```

---

## Tools & Standards
- **Modeler**: Camunda Modeler 5.49.0 (Camunda 8 / Zeebe and Camunda 7 compatible)
- **Standards**: BPMN 2.0, DMN 1.3, Camunda FEEL
- **Author**: Indra Adarsh ([@indra-adarsh](https://github.com/indra-adarsh))
