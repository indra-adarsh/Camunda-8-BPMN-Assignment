/**
 * Camunda 8 Mock Microservice Backend for PBJ-2 Labs 1, 2, and 3
 * Runs on Port 8088 (No third-party npm packages required; uses native Node.js http)
 */

const http = require('http');
const url = require('url');

const PORT = process.env.PORT || 8088;

function parseJsonBody(req) {
    return new Promise((resolve) => {
        let body = '';
        req.on('data', chunk => { body += chunk.toString(); });
        req.on('end', () => {
            try {
                resolve(body ? JSON.parse(body) : {});
            } catch (e) {
                resolve({});
            }
        });
    });
}

function sendJson(res, statusCode, data) {
    const payload = JSON.stringify(data);
    res.writeHead(statusCode, {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload),
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization'
    });
    res.end(payload);
}

const server = http.createServer(async (req, res) => {
    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;
    const method = req.method.toUpperCase();

    if (method === 'OPTIONS') {
        res.writeHead(204, {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type, Authorization'
        });
        return res.end();
    }

    const body = method === 'POST' ? await parseJsonBody(req) : {};
    console.log(`[${new Date().toISOString()}] ${method} ${pathname}`, body);

    // Health check
    if (pathname === '/' || pathname === '/health') {
        return sendJson(res, 200, { status: "UP", port: PORT, service: "Camunda 8 Mock Microservice" });
    }

    // =========================================================================
    // LAB 1: Loan Approval Automation Endpoints
    // =========================================================================
    if (pathname === '/api/applicant/validate') {
        const applicantId = body.applicantId || 'APP-1001';
        const requestedAmount = Number(body.requestedAmount || 25000);
        const monthlyIncome = Number(body.monthlyIncome || 6500);
        const existingMonthlyDebt = Number(body.existingMonthlyDebt || 1200);

        if (body.simulateWorkerFailure || applicantId.includes('FAIL')) {
            return sendJson(res, 500, { error: 'Simulated applicant validation service failure' });
        }

        const applicantValid = monthlyIncome > 0 && requestedAmount > 0;
        const dti = monthlyIncome > 0 ? Math.round(((existingMonthlyDebt / monthlyIncome) * 100) * 10) / 10 : 100.0;

        return sendJson(res, 200, {
            applicantValid: applicantValid,
            debtToIncomeRatio: dti,
            validationNotes: 'Applicant details successfully validated against core registry.'
        });
    }

    if (pathname === '/api/credit/assess') {
        const applicantId = body.applicantId || 'APP-1001';
        if (applicantId.includes('TIMEOUT')) {
            return sendJson(res, 504, { error: 'Downstream credit bureau SLA timeout' });
        }

        let creditScore = 780;
        let riskScore = 12;
        let remarks = 'Excellent credit profile. Zero delinquent payments in 36 months.';

        if (applicantId.includes('SUBPRIME')) {
            creditScore = 560;
            riskScore = 85;
            remarks = 'Subprime credit history. Multiple charge-offs detected.';
        } else if (applicantId.includes('REVIEW')) {
            creditScore = 660;
            riskScore = 48;
            remarks = 'Near-prime credit profile. Requires manual underwriter sign-off.';
        }

        return sendJson(res, 200, {
            applicantId: applicantId,
            creditScore: creditScore,
            riskScore: riskScore,
            remarks: remarks,
            bureauReference: 'BUR-2026-' + Math.random().toString(36).substring(2, 10).toUpperCase()
        });
    }

    if (pathname === '/api/loan/disburse') {
        const applicantId = body.applicantId || 'APP-1001';
        return sendJson(res, 200, {
            applicantId: applicantId,
            disbursementStatus: 'COMPLETED',
            disbursementRef: 'DISB-' + Math.random().toString(36).substring(2, 10).toUpperCase(),
            timestamp: Date.now()
        });
    }

    // =========================================================================
    // LAB 2: E-Commerce Order Processing Endpoints
    // =========================================================================
    if (pathname === '/api/order/validate') {
        const orderId = body.orderId || 'ORD-9001';
        const productId = body.productId || 'PROD-LAPTOP-01';
        const quantity = Number(body.quantity || 1);
        const unitPrice = Number(body.unitPrice || 149.99);

        if (body.simulateWorkerFailure) {
            return sendJson(res, 500, { error: 'Simulated warehouse inventory sync failure' });
        }

        const outOfStock = productId.toUpperCase().includes('OUT-OF-STOCK');
        return sendJson(res, 200, {
            orderValid: true,
            stockStatus: outOfStock ? 'OUT_OF_STOCK' : 'IN_STOCK',
            availableStock: outOfStock ? 0 : 150,
            orderTotal: Math.round(quantity * unitPrice * 100) / 100
        });
    }

    if (pathname === '/api/payment/process') {
        const orderId = body.orderId || 'ORD-9001';
        const token = body.paymentToken || '';

        if (token.includes('server_error_500') || orderId.includes('FAIL')) {
            return sendJson(res, 500, { error: 'Payment Gateway Communication Error (HTTP 500)' });
        }

        if (token.includes('declined')) {
            return sendJson(res, 200, {
                orderId: orderId,
                status: 'DECLINED',
                transactionId: null,
                gatewayMessage: 'Card declined: Insufficient funds',
                capturedAmount: 0.0
            });
        }

        return sendJson(res, 200, {
            orderId: orderId,
            status: 'SUCCESS',
            transactionId: 'TXN-' + Math.random().toString(36).substring(2, 10).toUpperCase() + '-CONFIRMED',
            gatewayMessage: 'Payment captured successfully via clearing house',
            capturedAmount: body.orderTotal || 149.99
        });
    }

    if (pathname === '/api/order/fulfill') {
        const orderId = body.orderId || 'ORD-9001';
        return sendJson(res, 200, {
            orderId: orderId,
            fulfillmentStatus: 'DISPATCHED',
            trackingNumber: 'TRK-' + Math.random().toString(36).substring(2, 10).toUpperCase()
        });
    }

    // =========================================================================
    // LAB 3: Hospital Patient Triage & Appointment Endpoints
    // =========================================================================
    if (pathname === '/api/patient/validate') {
        const patientRef = body.patientReference || 'PAT-98214';
        const prefDept = body.appointmentPreference || 'GENERAL';

        if (body.simulateWorkerFailure || patientRef.includes('FAIL')) {
            return sendJson(res, 500, { error: 'Simulated Master Patient Index (MPI) failure' });
        }

        const normalizedDept = prefDept.trim().toUpperCase().replace(/\s+/g, '_');
        return sendJson(res, 200, {
            registrationValid: true,
            normalizedDepartment: normalizedDept,
            urgencyScore: 50,
            validationNotes: 'Patient demographics verified against national registry.'
        });
    }

    if (pathname === '/api/hospital/scheduling/available-slots') {
        const patientRef = body.patientReference || 'PAT-98214';
        const dept = (body.department || 'GENERAL').toUpperCase();

        if (patientRef.includes('FAIL')) {
            return sendJson(res, 503, { error: 'Hospital scheduling subsystem temporarily offline' });
        }

        if (patientRef.includes('NOSLOT') || dept.includes('DERMATOLOGY')) {
            return sendJson(res, 200, {
                department: dept,
                slotsAvailable: false,
                slotId: null,
                slotTime: null,
                availableCount: 0
            });
        }

        const slotDept = String(body.department || 'GENERAL').toUpperCase();
        const slotPrefix = slotDept.substring(0, Math.min(slotDept.length, 5));
        const slotId = `SLOT-${slotPrefix}-${Math.floor(1000 + Math.random() * 9000)}`;
        const slotTime = new Date(Date.now() + 86400000).toISOString().replace(/\.\d+Z$/, '');

        return sendJson(res, 200, {
            department: dept,
            slotsAvailable: true,
            slotId: slotId,
            slotTime: slotTime,
            availableCount: 4
        });
    }

    if (pathname === '/api/hospital/scheduling/confirm-booking') {
        const patientRef = body.patientReference || 'PAT-98214';
        const slotId = body.slotId || 'SLOT-GEN-101';

        return sendJson(res, 200, {
            patientReference: patientRef,
            slotId: slotId,
            bookingStatus: 'CONFIRMED',
            bookingConfirmationCode: 'CONF-' + Math.random().toString(36).substring(2, 10).toUpperCase()
        });
    }

    // =========================================================================
    // GENERAL / SHARED: Notifications Dispatch Endpoint
    // =========================================================================
    if (pathname === '/api/notifications/send') {
        return sendJson(res, 200, {
            notificationSent: true,
            sentTimestamp: Date.now(),
            status: 'DELIVERED'
        });
    }

    // 404 Fallback
    console.warn(`[404] Not Found: ${method} ${pathname}`);
    return sendJson(res, 404, { error: `Endpoint ${pathname} not found` });
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`=================================================================`);
    console.log(` Camunda 8 Mock Microservice Backend is ONLINE on port ${PORT}`);
    console.log(` URL: http://localhost:${PORT}/`);
    console.log(` Ready to service REST Outbound Connectors for Labs 1, 2, and 3!`);
    console.log(`=================================================================`);
});
