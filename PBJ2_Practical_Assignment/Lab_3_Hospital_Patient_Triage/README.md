# Lab 3: End-to-End Hospital Appointment and Patient Triage Management

## 1. Problem Statement & Architecture
The objective of Lab 3 is to develop a patient triage and automated clinic scheduling workflow in **Camunda 8**. The system orchestrates:
- **Patient Registration & Intake Validation**: Synthetically validating patient identifiers, demographic parameters, and specialty clinic preferences using a Java/Spring Boot external Zeebe worker.
- **Evidence-Based Clinical Triage (DMN)**: Classifying patient presentations into `Urgent Assessment`, `Priority Review`, or `Routine` clinical tracks based on physiological indicators (severe chest pain, dyspnea, pain scale 1-10, pyrexia).
- **Fast-Track Emergency Escalation**: Routing emergent clinical cases directly to immediate medical resuscitation and admission user tasks in Tasklist.
- **REST Outbound Scheduling Integration**: Interfacing with a mock hospital scheduling API via Camunda 8 HTTP REST Outbound Connector to query live doctor appointment availability.
- **Manual Staff Scheduling Fallback**: Providing a dedicated Tasklist queue for triage officers and hospital registrars when overbooking authorization or waitlisting is necessary.
- **Incident Recovery & Monitoring**: Simulating scheduling system outages and validating zero-downtime recovery mechanics in Operate.

```
[Start: Patient Registration Ingested]
                 │
                 ▼
[Worker: Validate Registration] ──(Invalid)──► [Notify Error] ──► [End: Terminated]
                 │ (Valid)
                 ▼
[DMN: Classify Clinical Triage Table]
                 │
   ┌─────────────┼─────────────────────────────┐
(Urgent)     (Priority)                    (Routine)
   │             │                             │
   ▼             ▼                             │
[Tasklist:    [Tasklist:                       │
 Emergency     Authorised Staff Review]        │
 Resuscitation]  │                             │
   │             ▼                             │
   ▼      [REST Connector: Query Slots API] ◄──┘
[End: Admitted]  │
                 ▼
         [Slot Available?] ──(No)──► [Tasklist: Staff Waitlist] ──► [End: Waitlisted]
                 │ (Yes)
                 ▼
         [Confirm Booking]
                 │
                 ▼
  [End: Appointment Confirmed]
```

---

## 2. BPMN Model & Workflow Specification

- **BPMN File**: `models/lab3_hospital_patient_triage.bpmn`
- **Process ID**: `Process_HospitalPatientTriage`
- **Execution Platform**: Camunda Cloud 8.5+

### Workflow Activities & Task Types
1. **`Event_PatientRegistrationReceived` (Start Event)**: Ingests synthetic patient registration data (`patientReference`, `patientName`, `age`, `appointmentPreference`, `symptomsSummary`, `hasChestPain`, `hasBreathingDifficulty`, `painScale`, `feverTemperature`).
2. **`Activity_ValidatePatientRegistration` (Service Task)**:
   - **Job Type**: `validate-patient-registration`
   - **Worker Logic**: Validates patient demographic completeness, normalizes clinic department string, calculates baseline urgency score, sets `registrationValid`.
3. **`Gateway_RegistrationCheck` (Exclusive Gateway)**:
   - Condition: `= registrationValid = false` -> routes to `Activity_NotifyRegistrationError` -> terminates at `Event_End_InvalidRegistration`.
   - Default: Routes to `Activity_ClassifyTriageDMN`.
4. **`Activity_ClassifyTriageDMN` (Business Rule Task)**:
   - **Called Decision ID**: `decision_patient_triage`
   - **Result Variable**: `triageDecisionResult`
   - Maps `triageCategory`, `targetWaitMinutes`, `clinicalAction`.
5. **`Gateway_TriageBranching` (Exclusive Gateway)**:
   - Condition: `= triageCategory = "Urgent Assessment"` -> `Activity_UrgentClinicalAssessment` -> `Event_End_UrgentAdmitted`.
   - Condition: `= triageCategory = "Priority Review"` -> `Activity_PriorityStaffReview`.
   - Default: `= triageCategory = "Routine"` -> routes to `Activity_QueryAppointmentSlotsREST`.
6. **`Activity_UrgentClinicalAssessment` (User Task in Tasklist)**:
   - **Candidate Group**: `emergency-clinical-staff`
   - Form fields: Emergency resuscitation code, triage intervention notes, bed allocation.
7. **`Activity_PriorityStaffReview` (User Task in Tasklist)**:
   - **Candidate Group**: `triage-nursing-officers`
   - Reviewer signs off on clinical notes and clears case for priority slot search.
8. **`Activity_QueryAppointmentSlotsREST` (Service Task / REST Connector)**:
   - **Job Type**: `io.camunda:http-json:1`
   - **Method**: `POST`
   - **Target Endpoint**: `http://localhost:8080/api/hospital/scheduling/available-slots`
   - **Payload**: `={patientReference: patientReference, department: normalizedDepartment}`
   - **Outputs**: Maps `slotsAvailable`, `slotId`, `slotTime`, `availableCount`.
9. **`Gateway_SlotsAvailability` (Exclusive Gateway)**:
   - Condition: `= slotsAvailable = false` -> `Activity_ManualStaffScheduling` -> `Event_End_ManualWaitlist`.
   - Default: Routes to `Activity_ConfirmBooking` -> `Event_End_BookingConfirmed`.
10. **`Activity_ManualStaffScheduling` (User Task in Tasklist)**:
    - **Candidate Group**: `hospital-registrars`
    - Staff schedules manual overbooking slot or places patient on prioritized clinical waitlist.
11. **`Activity_ConfirmBooking` (Service Task)**:
    - Issues digital appointment pass and sends SMS confirmation to patient.

---

## 3. DMN Decision Table Specification

- **DMN File**: `models/lab3_patient_triage_decision.dmn`
- **Decision ID**: `decision_patient_triage`
- **Hit Policy**: `UNIQUE`

### Input & Output Definitions
| Parameter | TypeRef | Category | Description |
| :--- | :--- | :--- | :--- |
| `hasChestPain` | `boolean` | Input | Acute crushing retrosternal pain indicator |
| `hasBreathingDifficulty` | `boolean` | Input | Severe respiratory distress indicator |
| `painScale` | `number` | Input | Patient subjective pain assessment (1-10) |
| `feverTemperature` | `number` | Input | Patient core body temperature (°C) |
| `triageCategory` | `string` | Output | Clinical track: `"Urgent Assessment"`, `"Priority Review"`, `"Routine"` |
| `targetWaitMinutes` | `number` | Output | Maximum acceptable time to medical contact |
| `clinicalAction` | `string` | Output | Mandated clinical intervention directive |

### Decision Rules Table
| Rule # | Chest Pain? | Dyspnea? | Pain Scale | Temp (°C) | Triage Category | Target Wait | Directive |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **1** | `true` | `-` | `-` | `-` | `"Urgent Assessment"` | `10 min` | Immediate emergency cardiac resuscitation and telemetry |
| **2** | `false` | `true` | `-` | `-` | `"Urgent Assessment"` | `15 min` | Immediate acute respiratory critical care triage |
| **3** | `false` | `false` | `>= 8` | `-` | `"Priority Review"` | `30 min` | Severe acute pain protocol requiring priority clinical evaluation |
| **4** | `false` | `false` | `[5..7]` | `>= 39.0` | `"Priority Review"` | `45 min` | High febrile state with moderate distress requires urgent physician review |
| **5** | `false` | `false` | `[5..7]` | `< 39.0` | `"Priority Review"` | `60 min` | Moderate acuity triage scheduled for expedited same-day consultation |
| **6** | `false` | `false` | `< 5` | `>= 39.0` | `"Priority Review"` | `60 min` | Isolated high pyrexia requiring fast-track clinical evaluation |
| **7** | `false` | `false` | `< 5` | `< 39.0` | `"Routine"` | `120 min` | Stable non-urgent clinical presentation scheduled for standard outpatient slot |

---

## 4. Process-Variable Dictionary

| Variable Name | Type | Scope | Source / Activity | Description |
| :--- | :--- | :--- | :--- | :--- |
| `patientReference` | `String` | Process | Start Payload | Unique synthetic patient record ID (e.g. `PAT-2026-001`) |
| `patientName` | `String` | Process | Start Payload | Patient full legal name |
| `age` | `Integer` | Process | Start Payload | Age of patient |
| `appointmentPreference`| `String` | Process | Start Payload | Requested medical specialty department |
| `symptomsSummary` | `String` | Process | Start Payload | Summary narrative of clinical symptoms |
| `hasChestPain` | `Boolean` | Process | Start Payload | Predefined cardiac urgency indicator |
| `hasBreathingDifficulty`| `Boolean` | Process | Start Payload | Predefined respiratory urgency indicator |
| `painScale` | `Integer` | Process | Start Payload | Pain score (1 to 10) |
| `feverTemperature` | `Double` | Process | Start Payload | Body temperature in degrees Celsius |
| `registrationValid` | `Boolean` | Process | `Activity_ValidatePatientRegistration` | Demographics validation flag |
| `normalizedDepartment` | `String` | Process | `Activity_ValidatePatientRegistration` | Standardized department name |
| `urgencyScore` | `Integer` | Process | `Activity_ValidatePatientRegistration` | Normalized initial urgency index |
| `triageCategory` | `String` | Process | `Activity_ClassifyTriageDMN` | DMN outcome (`"Urgent Assessment"`, `"Priority Review"`, `"Routine"`) |
| `targetWaitMinutes` | `Integer` | Process | `Activity_ClassifyTriageDMN` | Target clinical response window |
| `clinicalAction` | `String` | Process | `Activity_ClassifyTriageDMN` | Recommended clinical triage action |
| `slotsAvailable` | `Boolean` | Process | `Activity_QueryAppointmentSlotsREST`| Flag indicating appointment availability |
| `slotId` | `String` | Process | `Activity_QueryAppointmentSlotsREST`| Reserved appointment slot code |
| `slotTime` | `String` | Process | `Activity_QueryAppointmentSlotsREST`| Timestamp for confirmed booking |
| `availableCount` | `Integer` | Process | `Activity_QueryAppointmentSlotsREST`| Total remaining slots for day |

---

## 5. REST Request / Response Examples

### Endpoint: `POST /api/hospital/scheduling/available-slots`
- **Request Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "patientReference": "PAT-2026-001",
    "department": "GENERAL_MEDICINE"
  }
  ```
- **Successful Response (Slot Available - HTTP 200 OK)**:
  ```json
  {
    "department": "GENERAL_MEDICINE",
    "slotsAvailable": true,
    "slotId": "SLOT-GM-1030",
    "slotTime": "2026-10-29T10:30:00Z",
    "availableCount": 5
  }
  ```
- **Response (No Slots Available - HTTP 200 OK)**:
  ```json
  {
    "department": "DERMATOLOGY",
    "slotsAvailable": false,
    "slotId": null,
    "slotTime": null,
    "availableCount": 0
  }
  ```
- **Simulated Service Failure Response (HTTP 503 Service Unavailable)**:
  ```json
  {
    "error": "Service Unavailable",
    "message": "Mock hospital scheduling subsystem is temporarily offline for maintenance",
    "timestamp": "2026-10-28T08:35:00Z"
  }
  ```

---

## 6. Test-Case Matrix & Verification Evidence

| Scenario ID | Test Input Highlights | Expected Gateway Route | Actual Outcome | Status |
| :--- | :--- | :--- | :--- | :---: |
| **Scenario 1** | Routine (Pain 2, Temp 37.2°C, Gen Med) | `Flow_RoutineTriage` -> `Flow_SlotFound` | Confirmed booking (SLOT-GM-1030) | **PASS** |
| **Scenario 2** | Priority (Pain 6, Temp 39.4°C) | `Flow_PriorityReview` -> Staff Review -> `Flow_SlotFound` | Clinical nurse signs off in Tasklist; slot booked | **PASS** |
| **Scenario 3** | Urgent (Chest pain = true, Pain 9) | `Flow_UrgentAssessment` -> Emergency User Task | Emergency Resuscitation & Admission | **PASS** |
| **Scenario 4** | Dermatology (0 available slots) | `Flow_RoutineTriage` -> `Flow_NoSlots` -> Tasklist Waitlist | Staff reviews in Tasklist and waitlists | **PASS** |
| **Scenario 5** | `simulateApiFailure = true` (HTTP 503) | REST Connector Incident in Operate | HTTP 503 incident logged; remediated; retried | **PASS** |

---

## 7. Operate Monitoring & Incident Investigation Guide

1. **Trigger Incident**:
   - Start instance with `simulateApiFailure = true`.
   - The REST connector to `http://localhost:8080/api/hospital/scheduling/available-slots` receives HTTP 503.
2. **Diagnose in Operate**:
   - Navigate to Camunda 8 Operate -> Incidents tab.
   - Inspect instance on `Activity_QueryAppointmentSlotsREST`: Error message: `HTTP 503 Service Unavailable`.
3. **Resolve & Retry**:
   - Restore mock service health or edit variable `simulateApiFailure` to `false`.
   - Click **Retry** on the incident.
   - The connector re-executes, successfully fetches slot `SLOT-ORTHO-1145`, and completes the workflow.
