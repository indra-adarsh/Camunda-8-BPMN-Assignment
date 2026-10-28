# Lab 1: End-to-End Loan Approval Automation

## 1. Problem Statement & Architecture
The objective of Lab 1 is to implement an automated, compliant, and observable loan origination and approval workflow in **Camunda 8**. The system combines:
- **BPMN 2.0 Orchestration**: Process flow management across automated service invocations, rule evaluations, and manual human fallbacks.
- **DMN 1.3 Business Rules**: Automated risk categorization and loan pricing using a hit-policy-driven decision table.
- **Java / Spring Boot External Job Worker**: Asynchronous validation of applicant records and computation of debt-to-income (DTI) metrics.
- **REST Outbound Connector**: Outbound integration with a credit-assessment scoring engine using synthetic financial credentials.
- **Tasklist Human Review**: Underwriter review queue for borderline risk tiers and manual exception handling.
- **Operate Monitoring & Incident Recovery**: Root cause analysis and retry mechanics for infrastructure or service timeouts.

```
[Start: Loan Application Submitted]
              │
              ▼
[Worker: Validate Applicant Data] ──(Invalid)──► [Notify Rejection] ──► [End: Terminated]
              │ (Valid)
              ▼
[REST Connector: Credit Assessment API]
              │
              ▼
[DMN: Loan Approval Decision Table]
              │
    ┌─────────┼─────────────────────────┐
(Approved) (Rejected)             (Manual Review)
    │         │                         │
    │         ▼                         ▼
    │   [Notify Rejection]   [Tasklist: Underwriter Review]
    │         │                         │
    │         ▼               ┌─────────┴─────────┐
    │   [End: Rejected]   (Approved)          (Rejected)
    ▼                         │                   │
[Disburse Loan Funds] ◄───────┘                   ▼
    │                                     [Notify Rejection]
    ▼                                             │
[End: Approved & Disbursed]                       ▼
                                           [End: Rejected]
```

---

## 2. BPMN Model & Workflow Specification

- **BPMN File**: `models/lab1_loan_approval_process.bpmn`
- **Process ID**: `Process_LoanApprovalAutomation`
- **Execution Platform**: Camunda Cloud 8.5+ (Zeebe engine)

### Workflow Activities & Task Types
1. **`Event_LoanAppReceived` (Start Event)**: Ingests the initial loan application payload containing applicant credentials, requested capital, and income details.
2. **`Activity_ValidateApplicant` (Service Task)**:
   - **Job Type**: `validate-loan-applicant`
   - **Retries**: 3
   - **Worker Logic**: Validates positive income, calculates `debtToIncomeRatio = (existingMonthlyDebt / monthlyIncome) * 100`, and sets `applicantValid = true/false`.
3. **`Gateway_ValidationCheck` (Exclusive XOR Gateway)**:
   - Default: `Flow_ValidationPassed` (routes to REST connector).
   - Condition: `= applicantValid = false` -> routes to `Activity_NotifyValidationFailed`.
4. **`Activity_CreditAssessmentREST` (Service Task / REST Connector)**:
   - **Job Type**: `io.camunda:http-json:1`
   - **Method**: `POST`
   - **Target Endpoint**: `http://localhost:8080/api/credit/assess`
   - **Payload**: `={applicantId: applicantId, requestedAmount: requestedAmount, monthlyIncome: monthlyIncome}`
   - **Outputs**: Maps `creditScore`, `riskScore`, and `creditAssessmentRemarks`.
5. **`Activity_EvaluateLoanDMN` (Business Rule Task)**:
   - **Called Decision ID**: `decision_loan_approval`
   - **Result Variable**: `loanDecisionResult`
   - **Mappings**: `decision`, `interestRate`, `decisionReason`.
6. **`Gateway_LoanDecision` (Exclusive XOR Gateway)**:
   - Conditional expression `= decision = "Approved"` -> `Activity_DisburseLoan`.
   - Conditional expression `= decision = "Rejected"` -> `Activity_NotifyRejected`.
   - Default flow: Routes to `Activity_ManualLoanReview`.
7. **`Activity_ManualLoanReview` (User Task in Tasklist)**:
   - **Candidate Group**: `loan-underwriters`
   - Form parameters: Displays applicant profile, credit score, DTI ratio, and requires `reviewDecision` ("Approved" / "Rejected") and `reviewerNotes`.
8. **`Gateway_ManualReviewOutcome` (Exclusive Gateway)**:
   - Conditional expression `= reviewDecision = "Approved"` -> `Activity_DisburseLoan`.
   - Default flow: Routes to `Activity_NotifyRejected`.
9. **`Activity_DisburseLoan` & `Activity_NotifyRejected` (Service Tasks)**:
   - Mock core banking disbursement and automated email/SMS dispatch.

---

## 3. DMN Decision Table Specification

- **DMN File**: `models/lab1_loan_decision_table.dmn`
- **Decision ID**: `decision_loan_approval`
- **Hit Policy**: `UNIQUE` (single unambiguous rule match per execution)

### Input & Output Definitions
| Parameter | TypeRef | Category | Description |
| :--- | :--- | :--- | :--- |
| `creditScore` | `number` | Input | Bureau credit rating (Range: 300 - 850) |
| `debtToIncomeRatio` | `number` | Input | Existing monthly debt divided by monthly income percentage (%) |
| `requestedAmount` | `number` | Input | Total loan principal amount requested ($) |
| `decision` | `string` | Output | Verdict: `"Approved"`, `"Rejected"`, or `"Manual Review"` |
| `interestRate` | `number` | Output | Annual percentage rate (APR %) assigned |
| `decisionReason` | `string` | Output | Documented compliance rationale |

### Decision Rules Table
| Rule # | Credit Score | Debt-to-Income (%) | Requested Amount ($) | Decision | Rate (%) | Reason |
| :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **1** | `>= 750` | `<= 35` | `<= 100000` | `"Approved"` | `5.5` | Prime credit rating and healthy DTI ratio qualify for instant automated approval |
| **2** | `[700..749]` | `<= 45` | `<= 50000` | `"Approved"` | `6.9` | Standard credit profile meets automated approval limits |
| **3** | `[620..699]` | `<= 45` | `<= 50000` | `"Manual Review"` | `8.5` | Near-prime credit profile requires underwriter manual review |
| **4** | `>= 700` | `(35..55]` | `> 50000` | `"Manual Review"` | `7.8` | Elevated debt-to-income or high balance requires manual underwriter review |
| **5** | `>= 750` | `<= 35` | `> 100000` | `"Manual Review"` | `6.2` | Jumbo loan amount requires senior credit committee manual sign-off |
| **6** | `< 620` | `-` | `-` | `"Rejected"` | `0.0` | Credit score is below the minimum institutional underwriting threshold of 620 |
| **7** | `>= 620` | `> 55` | `-` | `"Rejected"` | `0.0` | Debt-to-income ratio exceeds regulatory maximum limit of 55% |

---

## 4. Process-Variable Dictionary

| Variable Name | Type | Scope | Producer / Activity | Description |
| :--- | :--- | :--- | :--- | :--- |
| `applicantId` | `String` | Process | Start Payload | Unique synthetic applicant identifier (e.g. `APP-PRIME-101`) |
| `applicantName` | `String` | Process | Start Payload | Full name of the applicant |
| `requestedAmount` | `Double` | Process | Start Payload | Principal loan balance requested |
| `monthlyIncome` | `Double` | Process | Start Payload | Stated monthly verified gross income |
| `existingMonthlyDebt` | `Double` | Process | Start Payload | Existing monthly liabilities and debt obligations |
| `applicantValid` | `Boolean` | Process | `Activity_ValidateApplicant` | Flag denoting whether application passed initial sanity checks |
| `debtToIncomeRatio` | `Double` | Process | `Activity_ValidateApplicant` | Calculated DTI percentage: `(existingMonthlyDebt / monthlyIncome) * 100` |
| `validationNotes` | `String` | Process | `Activity_ValidateApplicant` | System audit remarks from validation worker |
| `creditScore` | `Long` | Process | `Activity_CreditAssessmentREST`| Credit rating returned by REST scoring engine |
| `riskScore` | `Integer` | Process | `Activity_CreditAssessmentREST`| Internal risk index (0-100) |
| `creditAssessmentRemarks`| `String` | Process | `Activity_CreditAssessmentREST`| Synthetic bureau assessment remarks |
| `decision` | `String` | Process | `Activity_EvaluateLoanDMN` | DMN outcome (`"Approved"`, `"Rejected"`, `"Manual Review"`) |
| `interestRate` | `Double` | Process | `Activity_EvaluateLoanDMN` | Risk-adjusted interest rate percentage |
| `decisionReason` | `String` | Process | `Activity_EvaluateLoanDMN` | Formal rationale for DMN decision |
| `reviewDecision` | `String` | Process | `Activity_ManualLoanReview` | Underwriter judgment entered in Tasklist |
| `reviewerNotes` | `String` | Process | `Activity_ManualLoanReview` | Written justifications by underwriter |
| `simulateWorkerFailure` | `Boolean` | Process | Test Input | Diagnostic flag to intentionally induce worker crash in Operate |
| `simulateApiTimeout` | `Boolean` | Process | Test Input | Diagnostic flag to simulate credit assessment REST timeout |

---

## 5. REST Request / Response Examples

### Endpoint: `POST /api/credit/assess`
- **Request Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "applicantId": "APP-PRIME-101",
    "requestedAmount": 35000,
    "monthlyIncome": 8000
  }
  ```
- **Successful Response (HTTP 200 OK)**:
  ```json
  {
    "applicantId": "APP-PRIME-101",
    "creditScore": 780,
    "riskScore": 12,
    "creditAssessmentRemarks": "Excellent credit profile. Zero delinquent payments in 36 months.",
    "bureauReference": "BUR-2026-99214"
  }
  ```
- **Error Response (HTTP 504 Gateway Timeout)**:
  ```json
  {
    "error": "Gateway Timeout",
    "message": "Downstream credit bureau did not respond within configured 5000ms SLA",
    "timestamp": "2026-10-28T08:30:00Z"
  }
  ```

---

## 6. Test-Case Matrix & Verification Evidence

| Scenario ID | Test Input Highlights | Expected Gateway Route | Actual Outcome | Status |
| :--- | :--- | :--- | :--- | :---: |
| **Scenario 1** | Score 780, DTI 25%, Loan $35,000 | `Flow_Approved` | Loan Approved (5.5% APR) & Disbursed | **PASS** |
| **Scenario 2** | Score 560, DTI 30%, Loan $25,000 | `Flow_Rejected` | Subprime rejection notification dispatched | **PASS** |
| **Scenario 3** | Score 660, DTI 40%, Loan $45,000 | `Flow_ManualReview` -> Underwriter Review -> Approved | Underwriter manually approves in Tasklist; disbursed | **PASS** |
| **Scenario 4** | `simulateApiTimeout = true` | REST Connector Incident in Operate | HTTP 504 incident diagnosed; retried successfully | **PASS** |
| **Scenario 5** | `simulateWorkerFailure = true` | Job Worker Incident in Operate | Retries exhausted incident logged; payload fixed; resolved | **PASS** |

---

## 7. Operate Monitoring & Incident Investigation Guide

1. **Incident Trigger**:
   - Start an instance with `simulateWorkerFailure = true`.
   - The worker `validate-loan-applicant` throws `RuntimeException("Simulated worker database connectivity failure")`.
2. **Operate Dashboard Inspection**:
   - Open Camunda 8 Operate (`http://localhost:8081` or Camunda SaaS cluster).
   - Filter by Process Definition: `Lab 1: End-to-End Loan Approval Automation`.
   - The instance shows a red incident badge on `Activity_ValidateApplicant`.
   - Incident Details: `Type: JOB_NO_RETRIES`, Error: `Simulated worker database connectivity failure`.
3. **Corrective Action & Recovery**:
   - In the Operate variable inspector, modify `simulateWorkerFailure` to `false` (or restore worker connectivity).
   - Click the **Retry** icon on the failed job incident.
   - The Zeebe engine immediately resets the job retries to 1, re-dispatches the work item, and the token transitions seamlessly to the next stage.
