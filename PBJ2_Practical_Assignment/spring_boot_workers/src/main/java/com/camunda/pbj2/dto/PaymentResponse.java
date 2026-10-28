package com.camunda.pbj2.dto;

public class PaymentResponse {
    private String orderId;
    private String paymentStatus;
    private String transactionId;
    private String paymentMessage;
    private Double settledAmount;

    public PaymentResponse() {}

    public PaymentResponse(String orderId, String paymentStatus, String transactionId, String paymentMessage, Double settledAmount) {
        this.orderId = orderId;
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
        this.paymentMessage = paymentMessage;
        this.settledAmount = settledAmount;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getPaymentMessage() { return paymentMessage; }
    public void setPaymentMessage(String paymentMessage) { this.paymentMessage = paymentMessage; }

    public Double getSettledAmount() { return settledAmount; }
    public void setSettledAmount(Double settledAmount) { this.settledAmount = settledAmount; }
}
