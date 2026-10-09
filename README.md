# Camunda 8 BPMN & DMN Process Modeling Repository

This repository contains production-ready BPMN 2.0 process models, DMN 1.3 decision tables, and comprehensive system design reports developed using Camunda 8 Modeler for enterprise and academic process orchestration.

---

## Repository Directory Structure

```
Camunda-8-BPMN-Assignment/
├── Project_Assignments/                                   # Advanced Enterprise & Academic BPMN Projects
│   ├── Assignment_1_Student_Project_Allocation/           # Assignment 1: Student Project Approval & Allocation
│   │   ├── Student_Project_Approval_and_Allocation.bpmn   # BPMN 2.0 model (Pool, 5 Lanes, Happy Path + F1-F11)
│   │   └── Assignment_1_Report.md                         # Detailed Failure Path Register (F1-F11) & Justifications
│   └── Assignment_3_Loan_Origination_Approval/            # Assignment 3: Loan Origination & Approval System
│       ├── Loan_Origination_and_Approval_System.bpmn      # BPMN 2.0 model (2 Pools, 6 Lanes, Happy Path + L1-L12)
│       └── Assignment_3_Report.md                         # Detailed Failure Path Register (L1-L12) & Justifications
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
