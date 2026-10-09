# Assignment 1: Student Project Approval & Allocation System

## 1. Executive Summary & Problem Context
In academic institutions, coordinating final-year capstone projects across hundreds of students and faculty members is prone to administrative bottlenecks, plagiarism, unvetted teams, mismatched guide expertise, and deadline lapses. This system orchestrates the complete project life cycle: from student proposal submission and algorithmic rule validation to committee evaluation, faculty guide matching, formal acceptance, and exception handling.

---

## 2. Process Boundaries & Participants

### 2.1 Process Boundaries
- **Starts when**: A student team decides to submit a project proposal.
- **Ends when**: A guide is formally allocated and confirmed, or the proposal is closed as rejected / lapsed.

### 2.2 Participants (Pools and Lanes)
The process is modeled inside a primary collaboration pool with five distinct horizontal swimlanes:
1. **Student / Team**: Submits initial proposals, handles form corrections, revises scope upon committee feedback, and reviews final allocation.
2. **Project Coordinator**: Screens proposals, adjudicates late submissions and duplicate topics, resolves escalated validation issues, and performs fallback guide allocation.
3. **Review Committee (incl. HoD)**: Conducts academic feasibility reviews, requests revisions, issues rejections, and handles SLA escalation to the Head of Department (HoD).
4. **Faculty Guide**: Reviews domain matching requests, accepts or declines assignments based on bandwidth/conflict of interest, and manages quota slots.
5. **Project Management System**: Executes automated completeness checks, team rule validations, similarity scanning, business rule guide allocation, quota reservation, notifications, and database records.

---

## 3. Happy Path Specification (10 Traceable Steps)

| Step # | Participant / Lane | Activity Name | Task Type | Description |
|:------:|:------------------:|:-------------:|:---------:|:------------|
| **1** | Student / Team | Submit Project Proposal | **User Task** | Team inputs project title, abstract, methodology, student roll numbers, and domain tags. |
| **2** | Project Management System | Validate Form Completeness & Team Rules | **Service Task** | Automated engine checks required fields, abstract length, team size limits (3–4), and verifies no student is enrolled in multiple teams. |
| **3** | Project Coordinator | Screen Proposal | **User Task** | Coordinator reviews preliminary eligibility and department scope alignment. |
| **4** | Project Management System | Run Similarity / Duplicate-Topic Check | **Service Task** | Plagiarism/similarity engine compares proposal against past 5 years of department projects. |
| **5** | Review Committee | Evaluate Project Proposal | **User Task** | Committee assesses technical merit, feasibility, and novelty, deciding to approve. |
| **6** | Project Management System | Check Guide Availability & Workload Match | **Business Rule Task** | DMN decision table matches project domain with faculty specialization and checks workload caps. |
| **7** | Project Management System | Allocate Guide & Reserve Quota | **Service Task + Send Task** | System reserves an allocation slot and sends formal request to selected guide. |
| **8** | Faculty Guide | Guide Reviews & Accepts Request | **User Task / Receive Task** | Faculty guide reviews team details and clicks Accept within the allocation sub-process. |
| **9** | Project Management System | Notify Student & Record Allocation | **Send Task + Service Task** | Automated notification dispatched to team with guide details; central database record committed. |
| **10** | All | End (Project Allocated & Confirmed) | **End Event** | Successful completion of allocation workflow. |

---

## 4. Failure Path Register (F1 – F11)

The following register details every failure point, trigger, BPMN solution construct, and architectural justification:

| ID | Failure Point | Cause / Trigger | Solution & BPMN Construct | Task Type & Gateway Justification |
|:---|:--------------|:----------------|:--------------------------|:-----------------------------------|
| **F1** | Proposal Validation | Missing fields, empty abstract, invalid student roll numbers. | Exclusive Gateway evaluates validation result. Generates an error list. Loops back to `Resubmit Corrected Proposal`. A loop counter limits attempts to 2; if exceeded, escalates to `Review Escalated Validation Errors` (Coordinator User Task). | **Exclusive Gateway (XOR)**: Routes mutually exclusive valid vs. invalid branches. **Service Task** generates error list. **User Task** allows human coordinator intervention. |
| **F2** | Team Rule Check | Team size exceeds cap, or student belongs to another registered team. | Validation identifies rule breach. System routes to `Notify Team Rule Violation` Service Task and terminates at an **Error End Event (`ERR_INVALID_TEAM`)** requiring team reformation. | **Error End Event**: Semantically indicates an unrecoverable structural fault in team formation that aborts the current instance. |
| **F3** | Duplicate / Plagiarized Topic | Similarity score exceeds department threshold (>25%). | System flags match to Coordinator User Task `Review Duplicate Match`. Exclusive Gateway offers two outcomes: (1) Request modified topic (loops back to submission task), or (2) Reject duplicate terminating at `End (Proposal Rejected - Plagiarism)`. | **User Task**: Plagiarism thresholds require human judgment. **Exclusive Gateway**: Clear split between revision and outright disqualification. |
| **F4** | Proposal Deadline | Student team fails to submit proposal before the official deadline. | **Interrupting Timer Boundary Event** attached to `Submit Project Proposal`. Diverts to Coordinator User Task `Review Late Submission Request`. Gateway allows late approval (returns to submission) or closes at `End (Proposal Lapsed)`. | **Interrupting Timer Boundary Event**: Prevents indefinite process stall when deadlines expire. **User Task**: Coordinator possesses discretionary authority for extensions. |
| **F5** | Committee Asks for Revision | Proposal scope is too large, objective is vague, or novelty is weak. | Committee Gateway selects "Revise". Student receives feedback with a **7-day timer**. Maximum 2 revision cycles enforced by `Gateway_RevisionCycles`. If exceeded, proposal is routed to rejection. | **Exclusive Gateway**: Enforces cycle cap. **User Task**: Allows iterative refinement within a strict SLA timer. |
| **F6** | Committee Rejects | Proposal is unfeasible, out of syllabus, or lacks lab resources. | Committee Gateway selects "Rejected". Service task dispatches detailed rejection reasons. Exclusive gateway grants 1 optional new topic submission attempt (loops to start); otherwise terminates at `End (Proposal Rejected)`. | **Exclusive Gateway**: Decides whether team has exhausted all fresh topic attempts. |
| **F7** | Review Delayed | Committee fails to convene or record evaluation within SLA. | **Non-Interrupting Timer Boundary Event** at 5 days sends a reminder email (`Send Committee SLA Reminder`). A secondary **Interrupting Timer Boundary Event** at 10 days triggers `Escalate to HoD for Expedited Review`. | **Non-Interrupting Timer**: Dispatches reminders without interrupting active evaluation. **Interrupting Timer**: Enforces hard administrative SLA escalation. |
| **F8** | No Guide Available | Domain mismatch or all matching faculty guides are at max quota. | DMN table returns `none`. Exclusive gateway diverts to Coordinator User Task `Allocate Guide Manually / Co-Guide / Waitlist`, merging back into the notification flow. | **Business Rule Task**: Evaluates guide capacity. **Exclusive Gateway**: Separates automated match from manual fallback. |
| **F9** | Guide Declines or is Silent | Guide has conflict of interest, excessive load, or ignores request for 3 days. | Inside `SubProcess_GuideAllocation`, a **3-day Timer Boundary Event** or explicit decline throws an **Error Event (`ERR_GUIDE_DECLINE`)**. Gateway allows up to 3 retries with alternative guides; thereafter escalates to Coordinator manual allocation. | **Error Boundary Event + Retry Loop**: Isolates allocation failures and enables clean fallback to manual intervention. |
| **F10** | Notification / System Failure | SMTP mail server failure or database connection pool exhaustion. | **Error Boundary Event (`ERR_NOTIF_FAIL`)** on the notification Service Task triggers fallback User Task `Send Manual Allocation Notification` by the Coordinator. | **Error Boundary Event**: Guarantees no student is left uninformed due to infrastructure failures. |
| **F11** | Withdrawal After Approval | Student team dissolves or guide takes emergency leave post-approval. | **Non-Interrupting Message Boundary Event (`Message_Withdrawal`)** on the allocation sub-process triggers a **Compensation Throw Event** invoking `Release Guide Quota Slot` to restore faculty capacity, then ends at `End (Project Withdrawn)`. | **Compensation Intermediate Throw Event + Compensation Handler (`isForCompensation="true"`)**: Reverses prior transactional state (frees quota slot). |

---

## 5. Gateway and Task Type Justifications

### 5.1 Gateway Selections
- **Exclusive Gateways (XOR)**: Used for validation checks, committee evaluation (Approve / Revise / Reject), duplicate resolution, attempt limits, and guide acceptance where exactly one outcome path must be taken.
- **Loop Counters**: Modeled via Exclusive Gateways comparing process variables (e.g., `revisionCycles <= 2`, `attempts <= 2`, `guideAttempts < 3`).

### 5.2 Task Type Selections
- **User Tasks**: Employed where human subjective judgment, reviews, or signatures are required (Student submissions, Coordinator screening, Committee evaluation, Guide response).
- **Service Tasks**: Applied for automated computation, rules validation, similarity checks, and quota reservation.
- **Business Rule Tasks**: Used for automated decision table evaluation (e.g., guide matching against domain tags and quota matrix).
- **Send Tasks**: Used for asynchronous outbound communications (committee reminders, guide invitations, student confirmations).
- **Sub-Processes & Compensation**: Encapsulates transactional guide reservation and release semantics.
