# Camunda 8 Spring Boot External Workers & Mock REST API

This Spring Boot application provides:
1. **Zeebe External Job Workers** for Labs 1, 2, and 3:
   - `validate-loan-applicant` (Lab 1)
   - `loan-disbursement-service` (Lab 1)
   - `validate-order-inventory` (Lab 2)
   - `fulfillment-service` (Lab 2)
   - `validate-patient-registration` (Lab 3)
   - `booking-confirmation-service` (Lab 3)
   - `notification-service` (Shared)
2. **Mock REST API Endpoints** for Camunda 8 REST Connectors:
   - `POST /api/credit/assess` (Lab 1 Credit Assessment API)
   - `POST /api/payment/process` (Lab 2 Payment Gateway API)
   - `POST /api/hospital/scheduling/available-slots` (Lab 3 Hospital Scheduling API)
3. **Simulated Failure Triggers** to demonstrate Operate Incident Monitoring & Recovery.

---

## Prerequisites
- Java 17 or higher
- Apache Maven 3.8+ (or Maven wrapper)
- Running Camunda 8 cluster (SaaS or Self-Managed via Docker Compose)

---

## Configuration (`application.yaml`)

### For Local Camunda 8 Self-Managed (Default)
```yaml
zeebe:
  client:
    broker:
      gateway-address: 127.0.0.1:26500
    security:
      plaintext: true
```

### For Camunda 8 SaaS (Camunda Cloud)
Set the following environment variables:
```bash
export ZEEBE_GATEWAY_ADDRESS=your-cluster-id.region.zeebe.camunda.io:443
export ZEEBE_PLAINTEXT=false
export ZEEBE_CLIENT_ID=your-client-id
export ZEEBE_CLIENT_SECRET=your-client-secret
export ZEEBE_CLUSTER_ID=your-cluster-id
export ZEEBE_REGION=bru-2
```

---

## Build & Run Instructions

```bash
cd spring_boot_workers

# Compile and package
mvn clean package -DskipTests

# Run application on port 8080
mvn spring-boot:run
```

Once running, verify endpoints:
```bash
# Health check / Test Lab 1 Credit Assessment API
curl -X POST http://localhost:8080/api/credit/assess \
  -H "Content-Type: application/json" \
  -d '{"applicantId": "APP-PRIME-101", "requestedAmount": 35000, "monthlyIncome": 8000}'

# Test Lab 2 Payment Gateway API
curl -X POST http://localhost:8080/api/payment/process \
  -H "Content-Type: application/json" \
  -d '{"orderId": "ORD-101", "orderTotal": 2400.0, "paymentToken": "tok_visa_valid_9988"}'

# Test Lab 3 Hospital Scheduling API
curl -X POST http://localhost:8080/api/hospital/scheduling/available-slots \
  -H "Content-Type: application/json" \
  -d '{"patientReference": "PAT-001", "department": "GENERAL_MEDICINE"}'
```
