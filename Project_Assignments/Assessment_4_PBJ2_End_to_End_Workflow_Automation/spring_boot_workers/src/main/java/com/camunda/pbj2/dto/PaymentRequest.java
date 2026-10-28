package com.camunda.pbj2.dto;

public class PaymentRequest {
    private String orderId;
    private Double orderTotal;
    private String paymentToken;

    public PaymentRequest() {}

    public PaymentRequest(String orderId, Double orderTotal, String paymentToken) {
        this.orderId = orderId;
        this.orderTotal = orderTotal;
        this.paymentToken = paymentToken;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Double getOrderTotal() { return orderTotal; }
    public void setOrderTotal(Double orderTotal) { this.orderTotal = orderTotal; }

    public String getPaymentToken() { return paymentToken; }
    public void setPaymentToken(String paymentToken) { this.paymentToken = paymentToken; }
}
