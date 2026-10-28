package com.camunda.pbj2.dto;

public class CreditAssessmentResponse {
    private String applicantId;
    private Long creditScore;
    private Integer riskScore;
    private String creditAssessmentRemarks;
    private String bureauReference;

    public CreditAssessmentResponse() {}

    public CreditAssessmentResponse(String applicantId, Long creditScore, Integer riskScore, String remarks, String ref) {
        this.applicantId = applicantId;
        this.creditScore = creditScore;
        this.riskScore = riskScore;
        this.creditAssessmentRemarks = remarks;
        this.bureauReference = ref;
    }

    public String getApplicantId() { return applicantId; }
    public void setApplicantId(String applicantId) { this.applicantId = applicantId; }

    public Long getCreditScore() { return creditScore; }
    public void setCreditScore(Long creditScore) { this.creditScore = creditScore; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getCreditAssessmentRemarks() { return creditAssessmentRemarks; }
    public void setCreditAssessmentRemarks(String creditAssessmentRemarks) { this.creditAssessmentRemarks = creditAssessmentRemarks; }

    public String getBureauReference() { return bureauReference; }
    public void setBureauReference(String bureauReference) { this.bureauReference = bureauReference; }
}
