# Camunda 8 BPMN & DMN Process Modeling Repository

This repository contains production-ready BPMN 2.0 process models, DMN 1.3 decision tables, and comprehensive system design reports developed using Camunda 8 Modeler for enterprise and academic process orchestration.

---

## Repository Directory Structure

```
Camunda-8-BPMN-Assignment/
├── PBJ2_Practical_Assignment/                             # PBJ-2 Practical Assignment: End-to-End Automation
│   ├── Lab_1_Loan_Approval_Automation/                    # Lab 1: Loan Approval Automation
│   │   ├── models/ (lab1_loan_approval_process.bpmn, lab1_loan_decision_table.dmn)
│   │   ├── test_payloads/ (5 JSON test scenarios: normal, subprime, manual review, timeout, incident)
│   │   └── README.md (Lab 1 report, DMN rules, variable dictionary, incident recovery)
│   ├── Lab_2_Ecommerce_Order_Processing/                  # Lab 2: E-Commerce Order Processing & Fulfilment
│   │   ├── models/ (lab2_order_processing_fulfillment.bpmn, lab2_order_priority_rules.dmn)
│   │   ├── test_payloads/ (5 JSON test scenarios: in-stock, out-of-stock, declined, review, incident)
│   │   └── README.md (Lab 2 report, priority rules, gateway routing, incident recovery)
│   ├── Lab_3_Hospital_Patient_Triage/                     # Lab 3: Hospital Appointment & Patient Triage
│   │   ├── models/ (lab3_hospital_patient_triage.bpmn, lab3_patient_triage_decision.dmn)
│   │   ├── test_payloads/ (5 JSON test scenarios: routine, priority, urgent ER, no-slots, incident)
│   │   └── README.md (Lab 3 report, triage rules, routing, incident recovery)
│   ├── spring_boot_workers/                               # Java 17 / Spring Boot 3.2 External Workers & Mock APIs
│   │   ├── pom.xml, application.yaml, Pbj2Camunda8Application.java
│   │   ├── workers/ (LoanApplicantWorker, OrderValidationWorker, PatientRegistrationWorker)
│   │   └── controller/ (CreditAssessmentMockController, PaymentGatewayMockController, HospitalSchedulingMockController)
│   ├── tests/ (test_pbj2_labs.py - Automated verification test suite)
│   └── PBJ2_PRACTICAL_ASSIGNMENT_REPORT.md                # Comprehensive Submission Report for Labs 1-3
├── Project_Assignments/                                   # Advanced Enterprise & Academic BPMN Projects
│   ├── Assignment_1_Student_Project_Allocation/           # Assignment 1: Student Project Approval & Allocation
│   │   ├── Student_Project_Approval_and_Allocation.bpmn   # BPMN 2.0 model (Pool, 5 Lanes, Happy Path + F1-F11)
│   │   └── Assignment_1_Report.md                         # Detailed Failure Path Register (F1-F11) & Justifications
│   ├── Assignment_3_Loan_Origination_Approval/            # Assignment 3: Loan Origination & Approval System
│   │   ├── Loan_Origination_and_Approval_System.bpmn      # BPMN 2.0 model (2 Pools, 6 Lanes, Happy Path + L1-L12)
│   │   └── Assignment_3_Report.md                         # Detailed Failure Path Register (L1-L12) & Justifications
│   └── Assessment_4_PBJ2_End_to_End_Workflow_Automation/  # Assessment 4: Camunda 8 Labs 1-3 End-to-End Automation
│       ├── Lab_1_Loan_Approval_Automation/                # Lab 1: Models, Payloads, Report
│       ├── Lab_2_Ecommerce_Order_Processing/              # Lab 2: Models, Payloads, Report
│       ├── Lab_3_Hospital_Patient_Triage/                 # Lab 3: Models, Payloads, Report
│       ├── spring_boot_workers/                           # Java/Spring Boot Zeebe workers & mock REST APIs
│       └── PBJ2_PRACTICAL_ASSIGNMENT_REPORT.md            # Comprehensive Assessment 4 Report
├── DMN_BPMN_Assignment/                                   # Decision Management & Business Rule Tasks
│   ├── 01_Loan_Application_Risk_Routing.bpmn              # Exercise 1: Single Result DMN + XOR Routing
│   ├── 01_evaluate_loan_risk.dmn                          # DMN 1.3 Table (Unique Hit Policy, FEEL expressions)
│   ├── 02_Order_Discount_Fulfillment.bpmn                 # Exercise 2: Collect Sum DMN + Script Task Calculation
│   ├── 02_calculate_discounts.dmn                         # DMN 1.3 Table (Collect Sum Hit Policy C+)
│   └── README.md                                          # Detailed DMN specification documentation
├── BPMN_Assignment/                                       # Core BPMN Workflows
│   ├── 01_Employee_Leave_Approval.bpmn                    # Scenario 1: Leave Approval
│   ├── 02_Online_Purchase_Order.bpmn                      # Scenario 2: Purchase Order
│   └── 03_IT_Service_Request.bpmn                         # Scenario 3: IT Service Request
├── tests/                                                 # Automated Verification & Test Suite
│   ├── test_dmn_and_bpmn.py                               # Unit tests for DMN Decision Tables & FEEL logic
│   └── test_project_assignments.py                        # Validation for Pools, Lanes, Boundary Events & Flows
└── README.md                                              # Repository Master Documentation
```

---

## Detailed Project Descriptions

### 🌟 Assessment 4: PBJ-2 Practical Assignment (Camunda 8 Labs 1–3 End-to-End Automation) ([Project_Assignments/Assessment_4_PBJ2_End_to_End_Workflow_Automation/](Project_Assignments/Assessment_4_PBJ2_End_to_End_Workflow_Automation/))
*Master Implementation & Engineering Report: [PBJ2_PRACTICAL_ASSIGNMENT_REPORT.md](Project_Assignments/Assessment_4_PBJ2_End_to_End_Workflow_Automation/PBJ2_PRACTICAL_ASSIGNMENT_REPORT.md)*

- **Lab 1: End-to-End Loan Approval Automation** ([PBJ2_Practical_Assignment/Lab_1_Loan_Approval_Automation/](PBJ2_Practical_Assignment/Lab_1_Loan_Approval_Automation/)):
  - Full BPMN orchestration combining Java/Spring Boot Zeebe job worker (`validate-loan-applicant`), DMN decision table (`decision_loan_approval`), Camunda 8 HTTP REST Outbound Connector (`POST /api/credit/assess`), Tasklist Underwriter user review, and Operate incident retry resolution.
  - 5 exhaustive test scenarios with synthetic JSON payloads covering prime approval, subprime rejection, near-prime manual review, API gateway timeout, and worker crash recovery.
- **Lab 2: End-to-End E-Commerce Order Processing and Fulfilment** ([PBJ2_Practical_Assignment/Lab_2_Ecommerce_Order_Processing/](PBJ2_Practical_Assignment/Lab_2_Ecommerce_Order_Processing/)):
  - Order-to-cash workflow featuring automated stock verification worker (`validate-order-inventory`), DMN order priority matrix (`decision_order_priority`), REST Outbound payment gateway connector (`POST /api/payment/process`), high-value manual verification in Tasklist (`orderTotal > $5,000`), and Operate incident recovery.
  - 5 test scenarios covering in-stock instant fulfillment, out-of-stock cancellation, payment decline, high-value review, and upstream payment gateway HTTP 500 recovery.
- **Lab 3: End-to-End Hospital Appointment and Patient Triage Management** ([PBJ2_Practical_Assignment/Lab_3_Hospital_Patient_Triage/](PBJ2_Practical_Assignment/Lab_3_Hospital_Patient_Triage/)):
  - Patient registration and emergency clinical triage workflow. Features registration validation worker (`validate-patient-registration`), DMN clinical triage table (`decision_patient_triage`), emergency fast-track resuscitation Tasklist task, mock hospital scheduling REST connector (`POST /api/hospital/scheduling/available-slots`), and staff waitlist fallback.
  - 5 test scenarios covering routine slot booking, high fever priority nursing review, cardiac urgent assessment admission, overbooked clinic manual waitlist, and scheduling API HTTP 503 incident recovery.
- **Spring Boot 3.2 External Workers & Mock APIs** ([PBJ2_Practical_Assignment/spring_boot_workers/](PBJ2_Practical_Assignment/spring_boot_workers/)):
  - Java 17+ microservice providing all Zeebe job workers and mock REST controllers with configurable failure simulation flags for Operate testing.

---

### 1. Assignment 1: Student Project Approval & Allocation System ([Project_Assignments/Assignment_1_Student_Project_Allocation/](Project_Assignments/Assignment_1_Student_Project_Allocation/))
- **Scenario**: Orchestration of academic capstone project approvals and faculty guide matching.
- **Participants (5 Swimlanes in Department Pool)**:
  - `Student / Team`: Submits proposals, performs resubmissions, and revises scope upon feedback.
  - `Project Coordinator`: Screens proposals, adjudicates late submission requests and duplicate topic matches, and handles manual guide allocation fallbacks.
  - `Review Committee (incl. HoD)`: Conducts technical evaluations, revision requests, rejections, and SLA escalation to the Head of Department (HoD).
  - `Faculty Guide`: Reviews allocation requests, accepts/declines based on bandwidth/conflicts within an allocation sub-process.
  - `Project Management System`: Automated completeness validation, team rule checks, similarity scoring, business rule guide allocation, quota reservation, and database logging.
- **Happy Path**: 10 traceable steps from proposal submission to confirmed guide allocation.
- **Failure Paths & BPMN Constructs Handled (F1 – F11)**:
  - `F1`: Proposal form validation error list, resubmission loop (max 2 attempts), and coordinator escalation.
  - `F2`: Team size/duplicate student rule violations terminating at Error End Event (`ERR_INVALID_TEAM`).
  - `F3`: Similarity/plagiarism detection with coordinator review (topic modification loop vs. outright rejection).
  - `F4`: Interrupting timer boundary event on proposal submission with late-submission coordinator approval.
  - `F5`: Committee revision request with a 7-day timer and maximum 2-cycle threshold.
  - `F6`: Committee rejection with structured reasons and 1-chance new topic submission loop.
  - `F7`: Non-interrupting timer boundary event for 5-day review reminder and 10-day interrupting SLA escalation to HoD.
  - `F8`: Automated guide matching returns "none" triggering coordinator manual allocation, co-guide, or waitlist.
  - `F9`: Guide silent (3-day timer) or explicit decline with up to 3 automated alternative retries before manual coordinator escalation.
  - `F10`: Error boundary event on email/database service task with automated fallback to manual coordinator dispatch.
  - `F11`: Post-approval team dissolution or guide emergency leave triggering Message Boundary Event + Compensation Handler to release quota slots.

---

### 2. Assignment 3: Loan Origination & Approval System ([Project_Assignments/Assignment_3_Loan_Origination_Approval/](Project_Assignments/Assignment_3_Loan_Origination_Approval/))
- **Scenario**: Multi-track bank loan origination handling personal and secured credit products from document intake to core banking disbursement.
- **Participants (2 Collaboration Pools, 6 Swimlanes)**:
  - `Credit Bureau (External Agency)`: External agency communicating via asynchronous BPMN Message Flows.
  - `Applicant`: Application intake, missing document upload, counter-offer review, sanction acceptance, and digital agreement e-signing.
  - `Loan Officer`: Initial file completeness, manual KYC discrepancy resolution, fallback manual credit checks, and counter-offer formulation.
  - `Underwriter / Credit Committee`: Risk policy checks, Senior Credit Committee escalation for high-value applications.
  - `Fraud & Compliance Team`: Dedicated sub-process investigating forged documents and AML/sanctions alerts.
  - `Operations / Disbursement`: Secured collateral valuation, title deeds verification, beneficiary account verification, and payment gateway fund transfers.
  - `Core Banking System`: Automated document validation, automated KYC, credit bureau API calls, risk scoring engine, loan offer generation, ledger account creation, and client notifications.
- **Happy Path**: 10 traceable steps from digital application submission to fund transfer and welcome kit dispatch.
- **Failure Paths & BPMN Constructs Handled (L1 – L12)**:
  - `L1`: Document deficiency feedback loop with a 7-day timer boundary event leading to lapsed status on non-response.
  - `L2`: Automated KYC identity discrepancy routed to manual Loan Officer verification (rejects if unresolved).
  - `L3`: Fraud/sanctions watchlist hit escalating to compliance sub-process and terminating immediately via Terminate End Event.
  - `L4`: External credit bureau API error boundary event with 3 automated retries and fallback manual bureau inquiry.
  - `L5 & L6`: Low credit score and high DTI ratio triggering counter-offer / guarantor / loan restructuring loop.
  - `L7`: Inclusive Gateway routing high-value or borderline loans to Senior Credit Committee with SLA timer.
  - `L8`: Secured loan collateral valuation and title defect verification (reduces LTV or rejects if title is clouded).
  - `L9`: Interrupting 15-day timer boundary event expiring unaccepted sanction offers.
  - `L10`: Process-wide applicant withdrawal message boundary event triggering compensation handlers to reverse fees and release collateral holds.
  - `L11`: Digital signature failure with up to 2 reminder cycles before cancellation.
  - `L12`: Payment gateway fund transfer error boundary event triggering account verification; if unresolvable, compensation throw event reverses Core Banking loan account creation.

---

### 3. Decision Management Module ([DMN_BPMN_Assignment/](DMN_BPMN_Assignment/))
- **Exercise 1 (Loan Risk Routing)**: Business Rule Task executing `evaluate-loan-risk` DMN table (`UNIQUE` hit policy) with single-result mapping and FEEL conditional sequence flow routing.
- **Exercise 2 (Order Discount & Fulfillment)**: Business Rule Task executing `calculate-discounts` DMN table (`COLLECT SUM` hit policy) aggregating multi-hit discounts, script task calculating final invoice amount, and XOR gateway routing.

---

### 4. Core BPMN Scenarios ([BPMN_Assignment/](BPMN_Assignment/))
- Three foundational workflows: Employee Leave Approval, Online Purchase Order, and IT Service Request.

---

## Automated Verification Suite

Run the full automated test suite covering all BPMN 2.0 schemas, XML well-formedness, pool/lane presence, flow connectivity, and DMN decision logic:

```bash
python -m unittest discover -s tests -p "test_*.py" -v
```

All 14 tests validate:
- Structural integrity and schema compliance for all `.bpmn` and `.dmn` files.
- Complete directional connectivity for all sequence flows, message flows, boundary events, and compensation associations.
- Boundary conditions and multi-hit aggregation for decision tables.

---

## Tools & Standards
- **Modeler**: Camunda Modeler 5.49.0 (Camunda 8 / Zeebe and Camunda 7 compatible)
- **Standards**: BPMN 2.0, DMN 1.3, Camunda FEEL
- **Author**: Indra Adarsh ([@indra-adarsh](https://github.com/indra-adarsh))
