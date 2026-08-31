import os
import unittest
import xml.etree.ElementTree as ET

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSIGNMENT_DIR = os.path.join(BASE_DIR, "DMN_BPMN_Assignment")

class TestCamundaModels(unittest.TestCase):

    def setUp(self):
        self.bpmn1_path = os.path.join(ASSIGNMENT_DIR, "01_Loan_Application_Risk_Routing.bpmn")
        self.dmn1_path = os.path.join(ASSIGNMENT_DIR, "01_evaluate_loan_risk.dmn")
        self.bpmn2_path = os.path.join(ASSIGNMENT_DIR, "02_Order_Discount_Fulfillment.bpmn")
        self.dmn2_path = os.path.join(ASSIGNMENT_DIR, "02_calculate_discounts.dmn")

    def test_files_exist(self):
        self.assertTrue(os.path.isfile(self.bpmn1_path), "01_Loan_Application_Risk_Routing.bpmn missing")
        self.assertTrue(os.path.isfile(self.dmn1_path), "01_evaluate_loan_risk.dmn missing")
        self.assertTrue(os.path.isfile(self.bpmn2_path), "02_Order_Discount_Fulfillment.bpmn missing")
        self.assertTrue(os.path.isfile(self.dmn2_path), "02_calculate_discounts.dmn missing")

    def test_xml_well_formedness(self):
        for path in [self.bpmn1_path, self.dmn1_path, self.bpmn2_path, self.dmn2_path]:
            try:
                tree = ET.parse(path)
                root = tree.getroot()
                self.assertIsNotNone(root, f"Root element is None for {path}")
            except Exception as e:
                self.fail(f"Failed to parse XML for {path}: {str(e)}")

    def test_exercise1_bpmn_elements(self):
        tree = ET.parse(self.bpmn1_path)
        root = tree.getroot()
        ns = {
            'bpmn': 'http://www.omg.org/spec/BPMN/20100524/MODEL',
            'zeebe': 'http://camunda.org/schema/zeebe/1.0',
            'camunda': 'http://camunda.org/schema/1.0/bpmn'
        }
        
        # Verify Business Rule Task configuration
        br_task = root.find('.//bpmn:businessRuleTask[@id="Activity_EvaluateLoanRisk"]', ns)
        self.assertIsNotNone(br_task, "Activity_EvaluateLoanRisk business rule task missing")
        
        called_decision = br_task.find('.//zeebe:calledDecision', ns)
        self.assertIsNotNone(called_decision, "zeebe:calledDecision element missing")
        self.assertEqual(called_decision.attrib.get('decisionId'), 'evaluate-loan-risk')
        self.assertEqual(called_decision.attrib.get('resultVariable'), 'loanRiskDecision')
        
        # Verify Exclusive Gateway
        gateway = root.find('.//bpmn:exclusiveGateway[@id="Gateway_RiskEvaluation"]', ns)
        self.assertIsNotNone(gateway, "Gateway_RiskEvaluation missing")
        
        # Verify XOR conditions
        cond_low = root.find('.//bpmn:sequenceFlow[@id="Flow_AutoApprove"]/bpmn:conditionExpression', ns)
        self.assertIsNotNone(cond_low, "AutoApprove condition missing")
        self.assertIn('riskTier = "LOW"', cond_low.text)
        
        cond_med = root.find('.//bpmn:sequenceFlow[@id="Flow_UnderwriterReview"]/bpmn:conditionExpression', ns)
        self.assertIsNotNone(cond_med, "UnderwriterReview condition missing")
        self.assertIn('riskTier = "MEDIUM"', cond_med.text)
        self.assertIn('requiresManualReview = true', cond_med.text)
        
        cond_high = root.find('.//bpmn:sequenceFlow[@id="Flow_AutoReject"]/bpmn:conditionExpression', ns)
        self.assertIsNotNone(cond_high, "AutoReject condition missing")
        self.assertIn('riskTier = "HIGH"', cond_high.text)
        self.assertIn('requiresManualReview = false', cond_high.text)

    def test_exercise1_dmn_table_structure(self):
        tree = ET.parse(self.dmn1_path)
        root = tree.getroot()
        ns = {'dmn': 'https://www.omg.org/spec/DMN/20191111/MODEL/'}
        
        decision = root.find('.//dmn:decision[@id="evaluate-loan-risk"]', ns)
        self.assertIsNotNone(decision, "evaluate-loan-risk decision missing")
        
        table = decision.find('./dmn:decisionTable', ns)
        self.assertIsNotNone(table, "Decision table missing")
        self.assertEqual(table.attrib.get('hitPolicy'), 'UNIQUE')
        
        rules = table.findall('./dmn:rule', ns)
        self.assertEqual(len(rules), 4, "Expected exactly 4 rules in evaluate-loan-risk")

    def test_exercise2_bpmn_elements(self):
        tree = ET.parse(self.bpmn2_path)
        root = tree.getroot()
        ns = {
            'bpmn': 'http://www.omg.org/spec/BPMN/20100524/MODEL',
            'zeebe': 'http://camunda.org/schema/zeebe/1.0',
            'camunda': 'http://camunda.org/schema/1.0/bpmn'
        }
        
        # Verify Business Rule Task for Collect Sum discount
        br_task = root.find('.//bpmn:businessRuleTask[@id="Activity_CalculateTotalDiscount"]', ns)
        self.assertIsNotNone(br_task, "Activity_CalculateTotalDiscount missing")
        called_decision = br_task.find('.//zeebe:calledDecision', ns)
        self.assertEqual(called_decision.attrib.get('decisionId'), 'calculate-discounts')
        self.assertEqual(called_decision.attrib.get('resultVariable'), 'totalDiscount')
        
        # Verify Script Task
        script_task = root.find('.//bpmn:scriptTask[@id="Activity_CalculateFinalAmount"]', ns)
        self.assertIsNotNone(script_task, "Activity_CalculateFinalAmount script task missing")
        script = script_task.find('./bpmn:script', ns)
        self.assertIn("cartValue * (1 - (totalDiscount / 100))", script.text)
        
        # Verify XOR conditions
        cond_mgr = root.find('.//bpmn:sequenceFlow[@id="Flow_ManagerSignOff"]/bpmn:conditionExpression', ns)
        self.assertIn('finalAmount >= 1000', cond_mgr.text)
        
        cond_direct = root.find('.//bpmn:sequenceFlow[@id="Flow_ProceedDirectly"]/bpmn:conditionExpression', ns)
        self.assertIn('finalAmount < 1000', cond_direct.text)

    def test_exercise2_dmn_collect_sum(self):
        tree = ET.parse(self.dmn2_path)
        root = tree.getroot()
        ns = {'dmn': 'https://www.omg.org/spec/DMN/20191111/MODEL/'}
        
        decision = root.find('.//dmn:decision[@id="calculate-discounts"]', ns)
        self.assertIsNotNone(decision, "calculate-discounts decision missing")
        
        table = decision.find('./dmn:decisionTable', ns)
        self.assertEqual(table.attrib.get('hitPolicy'), 'COLLECT')
        self.assertEqual(table.attrib.get('aggregation'), 'SUM')
        
        rules = table.findall('./dmn:rule', ns)
        self.assertEqual(len(rules), 4, "Expected 4 rules in calculate-discounts")


# DMN Logic Simulation Tests

def evaluate_loan_risk(credit_score: int, loan_amount: float):
    # Rule 1: creditScore >= 750 and loanAmount <= 50000 -> "LOW", false
    if credit_score >= 750 and loan_amount <= 50000:
        return {"riskTier": "LOW", "requiresManualReview": False}
    # Rule 2: creditScore >= 750 and loanAmount > 50000 -> "MEDIUM", true
    elif credit_score >= 750 and loan_amount > 50000:
        return {"riskTier": "MEDIUM", "requiresManualReview": True}
    # Rule 3: creditScore between [600..749] -> "MEDIUM", true
    elif 600 <= credit_score <= 749:
        return {"riskTier": "MEDIUM", "requiresManualReview": True}
    # Rule 4: creditScore < 600 -> "HIGH", false
    elif credit_score < 600:
        return {"riskTier": "HIGH", "requiresManualReview": False}
    else:
        raise ValueError(f"Unmatched inputs: credit_score={credit_score}, loan_amount={loan_amount}")

def route_loan_decision(decision: dict):
    risk_tier = decision["riskTier"]
    requires_manual_review = decision["requiresManualReview"]
    
    if risk_tier == "LOW":
        return "Auto-Approve and Disburse"
    elif risk_tier == "MEDIUM" or requires_manual_review is True:
        return "Underwriter Review"
    elif risk_tier == "HIGH" and requires_manual_review is False:
        return "Auto-Reject Notification"
    else:
        raise ValueError("Invalid state for gateway routing")

def calculate_discounts(customer_tier: str, cart_value: float, promo_code: str):
    hits = []
    # Rule 1: customerTier == "PREMIUM" -> 10
    if customer_tier == "PREMIUM":
        hits.append(10)
    # Rule 2: cartValue >= 500 -> 5
    if cart_value >= 500:
        hits.append(5)
    # Rule 3: promoCode in ("FESTIVE10", "SPECIAL10") -> 10
    if promo_code in ("FESTIVE10", "SPECIAL10"):
        hits.append(10)
    # Rule 4: Default -> 0
    hits.append(0)
    
    # Collect Sum aggregation
    return sum(hits)

def calculate_final_amount(cart_value: float, total_discount: int):
    return cart_value * (1.0 - (total_discount / 100.0))

def route_fulfillment(final_amount: float):
    if final_amount >= 1000:
        return "Manager Sign-off"
    else:
        return "Proceed directly"


class TestBusinessLogicSimulation(unittest.TestCase):

    def test_exercise1_scenarios(self):
        # Scenario A: Excellent credit, low loan amount -> LOW, auto-approve
        res_a = evaluate_loan_risk(780, 30000)
        self.assertEqual(res_a["riskTier"], "LOW")
        self.assertFalse(res_a["requiresManualReview"])
        self.assertEqual(route_loan_decision(res_a), "Auto-Approve and Disburse")
        
        # Scenario B: Boundary 750, 50000 -> LOW, auto-approve
        res_b = evaluate_loan_risk(750, 50000)
        self.assertEqual(res_b["riskTier"], "LOW")
        self.assertFalse(res_b["requiresManualReview"])
        self.assertEqual(route_loan_decision(res_b), "Auto-Approve and Disburse")

        # Scenario C: High credit, large loan amount -> MEDIUM, underwriter review
        res_c = evaluate_loan_risk(800, 75000)
        self.assertEqual(res_c["riskTier"], "MEDIUM")
        self.assertTrue(res_c["requiresManualReview"])
        self.assertEqual(route_loan_decision(res_c), "Underwriter Review")

        # Scenario D: Fair credit [600..749] -> MEDIUM, underwriter review
        res_d1 = evaluate_loan_risk(600, 10000)
        self.assertEqual(res_d1["riskTier"], "MEDIUM")
        self.assertTrue(res_d1["requiresManualReview"])
        self.assertEqual(route_loan_decision(res_d1), "Underwriter Review")

        res_d2 = evaluate_loan_risk(749, 100000)
        self.assertEqual(res_d2["riskTier"], "MEDIUM")
        self.assertTrue(res_d2["requiresManualReview"])
        self.assertEqual(route_loan_decision(res_d2), "Underwriter Review")

        # Scenario E: Poor credit < 600 -> HIGH, auto-reject
        res_e1 = evaluate_loan_risk(599, 5000)
        self.assertEqual(res_e1["riskTier"], "HIGH")
        self.assertFalse(res_e1["requiresManualReview"])
        self.assertEqual(route_loan_decision(res_e1), "Auto-Reject Notification")

        res_e2 = evaluate_loan_risk(450, 200000)
        self.assertEqual(res_e2["riskTier"], "HIGH")
        self.assertFalse(res_e2["requiresManualReview"])
        self.assertEqual(route_loan_decision(res_e2), "Auto-Reject Notification")

    def test_exercise2_sample_payload(self):
        # Sample payload from assignment:
        # customerTier: "PREMIUM", cartValue: 600, promoCode: "FESTIVE10"
        total_discount = calculate_discounts("PREMIUM", 600, "FESTIVE10")
        self.assertEqual(total_discount, 25, "Final discount must be 25% (10 + 5 + 10 + 0)")
        
        final_amount = calculate_final_amount(600, total_discount)
        self.assertEqual(final_amount, 450.0)
        
        routing = route_fulfillment(final_amount)
        self.assertEqual(routing, "Proceed directly", "450 < 1000 proceeds directly to warehouse")

    def test_exercise2_additional_cases(self):
        # Case A: Regular customer, large cart, no promo -> 5% discount, finalAmount >= 1000
        disc_a = calculate_discounts("REGULAR", 1200, "NO_PROMO")
        self.assertEqual(disc_a, 5)
        amount_a = calculate_final_amount(1200, disc_a)
        self.assertEqual(amount_a, 1140.0)
        self.assertEqual(route_fulfillment(amount_a), "Manager Sign-off")

        # Case B: Premium customer, promo SPECIAL10, small cart (< 500)
        disc_b = calculate_discounts("PREMIUM", 300, "SPECIAL10")
        self.assertEqual(disc_b, 20) # 10 (Premium) + 10 (Special10) + 0
        amount_b = calculate_final_amount(300, disc_b)
        self.assertEqual(amount_b, 240.0)
        self.assertEqual(route_fulfillment(amount_b), "Proceed directly")

        # Case C: Regular customer, cart < 500, no promo
        disc_c = calculate_discounts("REGULAR", 250, "")
        self.assertEqual(disc_c, 0)
        amount_c = calculate_final_amount(250, disc_c)
        self.assertEqual(amount_c, 250.0)
        self.assertEqual(route_fulfillment(amount_c), "Proceed directly")

if __name__ == '__main__':
    unittest.main()
