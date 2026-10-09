# Assignment 3: Loan Origination & Approval System

## 1. Executive Summary & Problem Context
Retail and commercial lending operations require balancing customer experience, rigorous risk underwriting, regulatory compliance (KYC, AML, fraud checks), and collateral valuation. This process models an end-to-end Loan Origination System (LOS) that handles personal and secured loan applications, incorporates multi-agency integrations (credit bureaus), supports parallel verification tracks, and implements comprehensive failure mitigation from initial document ingestion to core banking fund disbursement.

---

## 2. Process Boundaries & Participants

### 2.1 Process Boundaries
- **Starts when**: An applicant submits a loan application with supporting documentation (identity proof, income statements, collateral papers).
- **Ends when**: Funds are disbursed and the loan account is activated, or the application is rejected, withdrawn, or lapsed.

### 2.2 Participants (Pools and Lanes)
The architecture comprises two collaboration pools:
1. **External Pool: Credit Bureau (External Agency)**: Third-party credit scoring bureau communicating via asynchronous BPMN Message Flows.
2. **Main Pool: Bank Loan Origination & Approval System** featuring six functional swimlanes:
   - **Applicant**: Submits application, uploads missing documents, reviews counter-offers, accepts terms, e-signs contracts, or requests withdrawal.
   - **Loan Officer**: Conducts document pre-checks, investigates manual KYC mismatches, performs fallback manual credit evaluations, and formulates counter-offers.
   - **Underwriter / Credit Committee**: Conducts policy underwriting, reviews high-value/borderline applications, and coordinates senior credit committee approvals.
   - **Fraud & Compliance Team**: Investigates forged documentation, sanctions hits, and AML alerts in a dedicated compliance sub-process.
   - **Operations / Disbursement**: Oversees collateral valuation and title verification, validates disbursement bank accounts, executes fund transfers, and manages holds.
   - **Core Banking System**: Orchestrates automated document parsing, automated KYC screening, credit bureau API calls, automated risk scoring, contract generation, loan account ledger creation, and customer notifications.

---

## 3. Happy Path Specification (10 Traceable Steps)

| Step # | Participant / Lane | Activity Name | Task Type | Description |
|:------:|:------------------:|:-------------:|:---------:|:------------|
| **1** | Applicant | Submit Loan Application & Documents | **Message Start Event / User Task** | Applicant completes digital loan application and uploads KYC/income documents. |
| **2** | Core Banking System | Check Document Completeness | **Business Rule Task / Service Task** | Automated document ingestion engine checks completeness and legibility of submitted documents. |
| **3** | Multi-Lane | Parallel Verification Track | **Parallel Gateway (Fork & Join)** | Forks three concurrent verifications: (1) Automated KYC Identity Check (Core Banking), (2) Credit Bureau Score Retrieval (Core Banking via external pool), and (3) Income & Employment Verification (Loan Officer). |
| **4** | Core Banking System | Automated Fraud & AML Screening | **Business Rule Task** | Checks applicant against OFAC, PEP, and fraud watchlists. |
| **5** | Core Banking System | Eligibility & Risk Scoring Engine | **Business Rule Task** | Decision engine computes credit tier, debt-to-income (DTI) ratio, and routes to Auto-Approve. |
| **6** | Underwriter | Underwriter Review & Policy Checks | **User Task + Inclusive Gateway** | Underwriter confirms approval and validates policy checklists. |
| **7** | Core Banking System | Generate & Send Loan Offer | **Script Task + Send Task** | Generates sanction letter with approved amount, tenure, and APR, and transmits to applicant. |
| **8** | Applicant | Review & Accept Loan Offer | **User Task / Receive Task** | Applicant reviews terms and electronically accepts the loan offer. |
| **9** | Applicant | E-Sign Loan Agreement | **User Task** | Applicant executes digital promissory note and loan contract via digital signature gateway. |
| **10** | Operations / Core Banking | Create Loan Account & Disburse Funds | **Service Tasks + End Event** | Core banking creates loan ledger account, operations verifies beneficiary account, transfers funds, and dispatches welcome kit. |

---

## 4. Failure Path Register (L1 – L12)

The following register details all twelve failure scenarios, triggers, BPMN constructs, and design rationales:

| ID | Failure Point | Cause / Trigger | Solution & BPMN Construct | Task Type & Gateway Justification |
|:---|:--------------|:----------------|:--------------------------|:-----------------------------------|
| **L1** | Document Check | Incomplete, blurry, or expired documents. | Exclusive Gateway routes to `Generate Missing Items List` Service Task. Notifies applicant with a **7-day Timer Boundary Event**. If timer expires, terminates at `End (Application Lapsed)`. On resubmission, loops back to verification. | **Exclusive Gateway (XOR)**: Distinguishes complete vs. incomplete files. **Timer Boundary Event**: Prevents inactive files from remaining in active pipeline indefinitely. |
| **L2** | KYC Verification | National ID mismatch, name spelling discrepancy, or address mismatch. | Exclusive Gateway routes discrepancy to Loan Officer User Task `Manual KYC Investigation & Resolution`. If verified, rejoins main flow; if unresolved, terminates at `End (Rejected - KYC Mismatch)`. | **Exclusive Gateway**: Isolates automated pass from manual verification. **User Task**: Regulatory compliance mandates human review of discrepancies. |
| **L3** | Fraud / AML Screening | Forged pay stubs, altered bank statements, or international sanctions list match. | Fraud gateway detects hit, triggering `SubProcess_FraudInvestigation` in Fraud & Compliance lane. Investigators file a Suspicious Activity Report (SAR) and terminate the entire process via a **Terminate End Event (`TerminateEventDef_Fraud`)**. | **Terminate End Event**: Instantly kills all active concurrent branches (including parallel credit and income checks) to mitigate institutional liability. |
| **L4** | Credit Bureau Call | External Credit Bureau API timeout (HTTP 504) or agency server downtime. | Service Task features an **Error Boundary Event (`ERR_BUREAU_DOWN`)** with an exclusive retry gateway allowing up to 3 automated retries. If still down, falls back to Loan Officer User Task `Fallback Manual Credit Bureau Check`. | **Error Boundary Event + Retry Loop**: Protects against transient network glitches while providing graceful manual degradation. |
| **L5** | Credit Score / Risk | Credit bureau score is below minimum threshold for requested product. | Eligibility Exclusive Gateway routes to Loan Officer User Task `Prepare Counter-Offer / Guarantor Request`. Applicant reviews counter-offer: if accepted, loops back to eligibility scoring; if declined, terminates at `End (Counter-Offer Declined)`. | **Exclusive Gateway**: Provides a non-binary decision path between outright rejection and conditioned counter-offers. |
| **L6** | Income / DTI Ratio | Insufficient disposable income or debt-to-income ratio exceeds 50%. | Merged into the Counter-Offer flow (`Activity_PrepareCounterOffer`). Offers reduced loan principal or extended tenure (reducing monthly installment). Loops back for re-scoring. | **User Task + Feedback Loop**: Preserves conversion opportunities by re-evaluating risk with revised parameters. |
| **L7** | High-Value / Borderline Case | Loan amount exceeds underwriter's delegated authority, or score is borderline. | **Inclusive Gateway (`Gateway_AdditionalChecks`)** routes high-value applications to `Senior Credit Committee Approval` User Task with an SLA timer while concurrently proceeding with other required checks. | **Inclusive Gateway (OR)**: Allows conditional triggering of one, multiple, or no additional review tracks based on loan amount and policy criteria. |
| **L8** | Collateral Valuation | For secured loans: property valuation is lower than expected, or encumbrance/title defect detected. | Handled via **Inclusive Gateway** to Operations User Task `Collateral Valuation & Title Verification`. Exclusive gateway checks result: if title defect exists, terminates at `End (Rejected - Title Issues)`; if valuation is low, loan-to-value (LTV) is adjusted and merged to underwriting. | **User Task**: Specialist legal and surveyor appraisal require dedicated human workflows. |
| **L9** | Offer Acceptance Timeout | Applicant fails to accept loan sanction letter within offer validity period (15 days). | **Interrupting Timer Boundary Event** attached to `Review & Accept Loan Offer`. Automatically lapses the sanction letter and terminates at `End (Offer Expired)`. | **Interrupting Timer Boundary Event**: Cleans up expired credit commitments and frees capital allocations. |
| **L10** | Withdrawal at Any Stage | Applicant cancels application (found another lender, bought cash). | **Message Boundary Event (`Message_WithdrawLoan`)** on Underwriter Review routes to **Intermediate Compensation Throw Event** that executes compensation handlers (reversing processing fees and releasing collateral holds), ending at `End (Loan Withdrawn)`. | **Compensation Throw / Catch**: Enforces transactional integrity by releasing all institutional holds upon customer cancellation. |
| **L11** | E-Signature Failure | Applicant fails to sign digital promissory note or signature verification fails. | Exclusive gateway provides up to 2 reminder/resend cycles (`Flow_ESign_Retry`). If signature is not completed after retries, cancels offer and terminates at `End (Offer Cancelled - Signature Failed)`. | **Exclusive Gateway**: Implements finite retry policy before terminating stalled deals. |
| **L12** | Disbursement Failure | Invalid beneficiary account number, IFSC/routing error, or payment gateway crash. | **Error Boundary Event (`ERR_PAYMENT_GATEWAY`)** on fund transfer triggers `Resolve Account Discrepancy & Hold Funds`. If unresolvable, fires a **Compensation Throw Event** that invokes `Reverse Loan Account Creation` on Core Banking to delete the loan ledger account, ending at `End (Disbursement Failed)`. | **Compensation Event Definition (`isForCompensation="true"`)**: Reverses prior accounting entries in the Core Banking ledger to prevent phantom debt creation. |

---

## 5. Gateway and Task Type Justifications

### 5.1 Gateway Architecture
- **Parallel Gateway (AND Fork / Join)**: Synchronizes independent verification tracks (KYC, Credit Bureau, Income). The join gateway guarantees that the risk engine executes only when all three verification artifacts are present.
- **Inclusive Gateway (OR Fork / Join)**: Efficiently handles conditional underwriting checks (Senior Credit Committee for high-value loans AND/OR Collateral valuation for secured loans).
- **Exclusive Gateway (XOR)**: Manages branching logic for binary decisions (pass/fail, accept/decline, retry/terminate).

### 5.2 Task Type Architecture
- **Business Rule Tasks**: Executes DMN decision tables for document completeness, fraud scoring, and credit risk evaluation.
- **Service Tasks**: Automates Core Banking operations, API calls to the Credit Bureau, and payment gateway fund transfers.
- **Send & Receive Tasks / Message Events**: Facilitates asynchronous communication with the applicant and external credit rating agencies.
- **User Tasks**: Assigns tasks requiring human diligence to specialized personnel (Loan Officers, Underwriters, Legal/Operations).
- **Sub-Processes**: Isolates complex multi-step procedures (Fraud & AML investigation) and establishes compensation boundaries.
