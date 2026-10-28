package com.camunda.pbj2.dto;

public class CreditAssessmentRequest {
    private String applicantId;
    private Double requestedAmount;
    private Double monthlyIncome;

    public CreditAssessmentRequest() {}

    public CreditAssessmentRequest(String applicantId, Double requestedAmount, Double monthlyIncome) {
        this.applicantId = applicantId;
        this.requestedAmount = requestedAmount;
        this.monthlyIncome = monthlyIncome;
    }

    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }

    public Double getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(Double requestedAmount) { this.requestedAmount = requestedAmount; }

    public Double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(Double monthlyIncome) { this.monthlyIncome = monthlyIncome; }
}
