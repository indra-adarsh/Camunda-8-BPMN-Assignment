# Camunda 8 BPMN Assignment

## Overview

This repository contains three BPMN process models created using **Camunda 8**. The models demonstrate basic BPMN building blocks including Start Events, Tasks, Exclusive Gateways, Sequence Flows, and End Events.

The three modeled scenarios are:

1. Employee Leave Approval
2. Online Purchase Order Processing
3. IT Service Request

---

## Scenario 1: Employee Leave Approval

### Process Description

The process begins when an employee submits a leave request through the company's HR system.

The HR system first checks the employee's available leave balance. An Exclusive Gateway determines whether sufficient leave balance is available.

* If sufficient leave balance is available, the request is sent to the manager for approval.
* If the leave balance is insufficient, the system sends an insufficient-balance notification and the process ends.

After the request reaches the manager, another Exclusive Gateway determines whether the manager approves or rejects the request.

* If approved, the system updates the employee's leave balance and sends an approval notification.
* If rejected, the system sends a rejection notification.

The process ends after the appropriate notification is sent.

### BPMN Elements Used

* Start Event
* User Tasks
* Service Tasks
* Exclusive Gateways
* Sequence Flows
* End Events

### BPMN Model

`BPMN/01_Employee_Leave_Approval.bpmn`

---

## Scenario 2: Online Purchase Order Processing

### Process Description

The process starts when a customer places an order.

The system checks whether the requested product is available. An Exclusive Gateway determines the next path.

* If the product is unavailable, the customer is notified that the product is out of stock and the process ends.
* If the product is available, the system proceeds to payment processing.

A second Exclusive Gateway checks whether the payment is successful.

* If payment fails, the customer is notified about the payment failure and the process ends.
* If payment is successful, the order is confirmed and the product is prepared for shipment.

The product is then shipped, the customer receives a shipping confirmation, and the process ends.

### BPMN Elements Used

* Start Event
* User Task
* Service Tasks
* Exclusive Gateways
* Multiple Process Paths
* Sequence Flows
* End Events

### BPMN Model

`BPMN/02_Online_Purchase_Order.bpmn`

---

## Scenario 3: IT Service Request

### Process Description

The process begins when an employee submits an IT support request.

The IT help desk registers the request and checks the severity of the problem. An Exclusive Gateway determines whether the problem is low or high severity.

* A low-severity problem is assigned to a support technician.
* A high-severity problem is assigned to a senior technician.

Both paths then continue to the technician's investigation of the problem.

An Exclusive Gateway determines whether the problem can be resolved internally.

* If it can be resolved internally, the technician fixes the problem.
* If it cannot be resolved internally, the problem is escalated to the external service provider.

After the problem has been resolved, the help desk updates the request status. The employee then receives a resolution notification, and the process ends.

### BPMN Elements Used

* Start Event
* Multiple Tasks
* User Tasks
* Service Tasks
* Exclusive Gateways
* Alternative Paths
* Sequence Flows
* End Event

### BPMN Model

`BPMN/03_IT_Service_Request.bpmn`

---

## Repository Structure

```text
Camunda-8-BPMN-Assignment/
│
├── BPMN/
│   ├── 01_Employee_Leave_Approval.bpmn
│   ├── 02_Online_Purchase_Order.bpmn
│   └── 03_IT_Service_Request.bpmn
│
└── README.md
```

## Tool Used

**Camunda 8 Modeler**

## Submission

The repository contains the completed BPMN models for all three scenarios along with this documentation for evaluation.
