# Camunda 8 Simulation Guide: Assignment 1 & Assignment 3

This document provides complete instructions for executing and simulating **Assignment 1 (Student Project Approval & Allocation System)** and **Assignment 3 (Loan Origination & Approval System)** in **Camunda 8**.

---

## 1. Overview of Simulation Environments

Camunda 8 offers three primary ways to simulate and test these BPMN processes:

1. **Camunda 8 Web Modeler / Camunda Play (Recommended & Easiest)**:
   - Built-in zero-code visual execution engine directly in the browser.
   - Allows stepping through tasks interactively, filling form variables, and observing token movement across lanes in real time.
2. **Camunda Desktop Modeler**:
   - Open `.bpmn` files directly, deploy to Camunda 8 SaaS / Self-Managed cluster via Zeebe client credentials.
3. **Automated CLI / Python Simulation Engine (Included in Repository)**:
   - Instant token-based execution of Happy Paths and all Failure Scenarios without requiring an active cluster.

---

## 2. Running Simulations in Camunda 8 Web Modeler / Camunda Play

### Step 1: Import BPMN Models
1. Log into your Camunda 8 SaaS Account (https://camunda.io) or local Web Modeler.
2. Navigate to your project folder and click **Upload / Import**.
3. Select:
   - `Student_Project_Approval_and_Allocation.bpmn` (Assignment 1)
   - `Loan_Origination_and_Approval_System.bpmn` (Assignment 3)

### Step 2: Launch Camunda Play
1. Open the diagram in Web Modeler.
2. Click the blue **Play** button in the top right corner.
3. This creates a sandbox instance where you can simulate each participant lane.

---

## 3. Assignment 1: Simulation Scenarios & Payloads

### Scenario 1.1: Happy Path (Seamless Approval & Guide Allocation)
- **Start Payload**:
  ```json
  {
    "projectTitle": "Autonomous Drone Fleet Coordination",
    "teamMembers": ["CS201", "CS202", "CS203"],
    "teamSize": 3,
    "domain": "Artificial Intelligence & Robotics",
    "abstractText": "This project investigates multi-agent reinforcement learning for autonomous drone fleet navigation and collision avoidance.",
    "lateSubmission": false
  }
  ```
- **Task Simulation Steps in Camunda Play**:
  1. `Validate Form Completeness`: Set `validationStatus = "VALID"`
  2. `Coordinator Screens Proposal`: Complete task.
  3. `Run Similarity Check`: Set `similarityScore = 12.5`
  4. `Review Committee Evaluates`: Set `committeeDecision = "APPROVED"`
  5. `Check Guide Availability`: Set `guideMatchFound = true`
  6. `Reserve Guide Quota`: Complete task.
  7. `Guide Reviews Request`: Set `guideResponse = "ACCEPT"`
  8. `Notify Student & Record`: Complete task.
- **Expected End Event**: `Event_End_Allocated` (Project Allocated & Confirmed).

### Scenario 1.2: Exception F1 (Form Validation Errors & Resubmission)
- **Start Payload**:
  ```json
  {
    "projectTitle": "Smart Traffic Signal Controller",
    "teamMembers": ["CS105", "CS106"],
    "teamSize": 2,
    "abstractText": ""
  }
  ```
- **Task Simulation Steps**:
  1. `Validate Form Completeness`: Set `validationStatus = "FORM_ERRORS"` and `resubmitAttempts = 1`.
  2. Process routes to `Return Error List`.
  3. Student completes `Resubmit Corrected Proposal`: Uploads full abstract and sets `teamSize = 3`.
  4. Second validation: Set `validationStatus = "VALID"`.
  5. Continues through happy path to `Event_End_Allocated`.

### Scenario 1.3: Exception F2 (Invalid Team Rule Breach)
- **Task Simulation Steps**:
  1. `Validate Form Completeness`: Set `validationStatus = "INVALID_TEAM"`.
  2. System executes `Notify Team Rule Violation`.
  3. **Expected End Event**: `Event_End_InvalidTeam` (Terminates with Error `ERR_INVALID_TEAM`).

### Scenario 1.4: Exception F5 (Committee Revision Loop)
- **Task Simulation Steps**:
  1. `Review Committee Evaluates`: Set `committeeDecision = "REVISE"`.
  2. Student completes `Revise Proposal`: Set `revisionCycles = 1`.
  3. Gateway checks `revisionCycles <= 2` -> loops back to committee.
  4. Committee second review: Set `committeeDecision = "APPROVED"`.
  5. Proceeds to Guide Allocation.

### Scenario 1.5: Exception F9 (Guide Declines & System Reruns Match)
- **Task Simulation Steps**:
  1. In `SubProcess_GuideAllocation`, Guide completes `Guide Reviews Request`: Set `guideResponse = "DECLINE"`.
  2. Sub-process throws `ERR_GUIDE_DECLINE`.
  3. Gateway checks `guideDeclineAttempts < 3` -> loops back to `Check Guide Availability & Workload`.
  4. Second guide assigned -> sets `guideResponse = "ACCEPT"`.
  5. Reaches `Event_End_Allocated`.

---

## 4. Assignment 3: Simulation Scenarios & Payloads

### Scenario 3.1: Happy Path (Instant Auto-Approve & Fund Transfer)
- **Start Payload**:
  ```json
  {
    "applicantName": "Sarah Jenkins",
    "requestedAmount": 35000.0,
    "loanType": "PERSONAL",
    "incomeMonthly": 7500.0,
    "isSecuredLoan": false,
    "isStandardLoan": true,
    "requiresSeniorCommittee": false
  }
  ```
- **Task Simulation Steps in Camunda Play**:
  1. `Check Document Completeness`: Set `docsComplete = true`.
  2. Parallel Gateway triggers 3 concurrent tasks:
     - `Automated KYC Check`: Set `kycMatched = true`
     - `Retrieve Credit Bureau Score`: Set `creditScore = 810`, `bureauRetries = 0`
     - `Income Verification`: Complete task.
  3. Parallel Join activates `Fraud & AML Screening`: Set `fraudPassed = true`.
  4. `Eligibility & Risk Scoring`: Set `riskTier = "LOW"`, `approvedAmount = 35000.0`, `interestRate = 6.8`.
  5. Process routes straight to `Generate Formal Loan Offer` and `Send Loan Offer`.
  6. Applicant `Review & Accept Loan Offer`: Complete task.
  7. Applicant `E-Sign Loan Agreement`: Set `eSignStatus = "SIGNED"`.
  8. `Create Loan Account`: Complete task.
  9. `Verify Account & Transfer Funds`: Complete task.
  10. `Send Disbursement Notification`: Complete task.
- **Expected End Event**: `Event_End_LoanDisbursed`.

### Scenario 3.2: Exception L2 (KYC Discrepancy Resolved Manually)
- **Task Simulation Steps**:
  1. `Automated KYC Check`: Set `kycMatched = false`.
  2. Process routes to Loan Officer User Task `Manual KYC Investigation & Resolution`.
  3. Loan Officer checks physical passport copy and sets `kycResolved = true`.
  4. Workflow rejoins Parallel Join and continues to Risk Engine.

### Scenario 3.3: Exception L3 (AML Sanctions Watchlist Hit - Terminate)
- **Task Simulation Steps**:
  1. In `Automated Fraud & AML Screening`: Set `fraudPassed = false`.
  2. Process diverts to Compliance Sub-Process `Fraud & AML Compliance Investigation`.
  3. Officer executes `Analyze Forged Docs` and `File Regulatory SAR Report`.
  4. **Expected End Event**: `Event_End_TerminatedFraud` (Instantly kills all parallel tasks).

### Scenario 3.4: Exception L5 & L6 (Counter-Offer Restructuring)
- **Task Simulation Steps**:
  1. `Eligibility & Risk Scoring`: Set `riskTier = "COUNTER_OFFER"`.
  2. Loan officer prepares counter-offer: Reduced amount from $60K to $40K, 60-month tenure.
  3. Applicant reviews counter-offer: Sets `counterOfferAccepted = true`.
  4. Process loops back to `Eligibility & Risk Scoring` -> now evaluates to `riskTier = "LOW"`.
  5. Continues through offer and e-sign to disbursement.

### Scenario 3.5: Exception L12 (Disbursement Failure & Ledger Rollback)
- **Task Simulation Steps**:
  1. Loan account is created in Core Banking.
  2. `Verify Account & Transfer Funds` throws `ERR_PAYMENT_GATEWAY` (e.g., target account frozen).
  3. Operations executes `Resolve Account Discrepancy`: Sets `accountVerified = false`.
  4. Triggers Compensation Throw Event: Core Banking executes `Reverse Loan Account Creation` (`isForCompensation="true"`).
  5. **Expected End Event**: `Event_End_DisbursementFailed` (No orphan loan ledger remaining).

---

## 5. Running the Automated Python Simulation Engine

You can run all 12 simulation scenarios locally in the terminal with one command:

```powershell
python "C:\Users\indra\.gemini\antigravity\brain\b3bd3b19-0370-41e5-ade7-47aea2fb2d8a\scratch\run_simulations.py"
```

The detailed execution trace logs are recorded in:
- `simulation/assignment_1_simulation_results.log`
- `simulation/assignment_3_simulation_results.log`