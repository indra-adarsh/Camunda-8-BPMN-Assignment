package com.camunda.pbj2.dto;

public class SlotQueryRequest {
    private String patientReference;
    private String department;

    public SlotQueryRequest() {}

    public SlotQueryRequest(String patientReference, String department) {
        this.patientReference = patientReference;
        this.department = department;
    }

    public String getPatientReference() { return patientReference; }
    public void setPatientReference(String patientReference) { this.patientReference = patientReference; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
