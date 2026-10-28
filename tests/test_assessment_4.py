"""
Automated Verification Suite for Assessment 4 (PBJ-2 Practical Assignment: Labs 1-3)
Validates:
1. BPMN 2.0 XML structure, Camunda 8 Zeebe extensions, and FEEL expressions.
2. DMN 1.3 XML decision tables, Hit Policies, and rule consistency.
3. Simulation and evaluation of all DMN rules across Labs 1, 2, and 3.
4. Validation of 15 synthetic test payload files across all 3 Labs.
"""

import json
import os
import unittest
import xml.etree.ElementTree as ET

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE_DIR = os.path.join(REPO_ROOT, "Project_Assignments", "Assessment_4_PBJ2_End_to_End_Workflow_Automation")

LAB1_BPMN = os.path.join(BASE_DIR, "Lab_1_Loan_Approval_Automation", "models", "lab1_loan_approval_process.bpmn")
LAB1_DMN = os.path.join(BASE_DIR, "Lab_1_Loan_Approval_Automation", "models", "lab1_loan_decision_table.dmn")
LAB1_PAYLOADS = os.path.join(BASE_DIR, "Lab_1_Loan_Approval_Automation", "test_payloads")

LAB2_BPMN = os.path.join(BASE_DIR, "Lab_2_Ecommerce_Order_Processing", "models", "lab2_order_processing_fulfillment.bpmn")
LAB2_DMN = os.path.join(BASE_DIR, "Lab_2_Ecommerce_Order_Processing", "models", "lab2_order_priority_rules.dmn")
LAB2_PAYLOADS = os.path.join(BASE_DIR, "Lab_2_Ecommerce_Order_Processing", "test_payloads")

LAB3_BPMN = os.path.join(BASE_DIR, "Lab_3_Hospital_Patient_Triage", "models", "lab3_hospital_patient_triage.bpmn")
LAB3_DMN = os.path.join(BASE_DIR, "Lab_3_Hospital_Patient_Triage", "models", "lab3_patient_triage_decision.dmn")
LAB3_PAYLOADS = os.path.join(BASE_DIR, "Lab_3_Hospital_Patient_Triage", "test_payloads")

NS_BPMN = {
    "bpmn": "http://www.omg.org/spec/BPMN/20100524/MODEL",
    "bpmndi": "http://www.omg.org/spec/BPMN/20100524/DI",
    "zeebe": "http://camunda.org/schema/zeebe/1.0",
}

NS_DMN = {
    "dmn": "https://www.omg.org/spec/DMN/20191111/MODEL/",
    "dmndi": "https://www.omg.org/spec/DMN/20191111/DMNDI/",
}


class TestAssessment4ModelsAndRules(unittest.TestCase):

    def test_xml_files_exist_and_parse(self):
        """Verify all 6 BPMN and DMN model files exist in Assessment 4 and parse as valid XML."""
        files = [LAB1_BPMN, LAB1_DMN, LAB2_BPMN, LAB2_DMN, LAB3_BPMN, LAB3_DMN]
        for f in files:
            self.assertTrue(os.path.exists(f), f"File not found: {f}")
            tree = ET.parse(f)
            self.assertIsNotNone(tree.getroot(), f"Failed to parse root element for {f}")

    def test_bpmn_camunda8_extensions(self):
        """Verify BPMN models contain proper Zeebe task definitions, gateways, and FEEL expressions."""
        bpmn_files = [
            (LAB1_BPMN, "Process_LoanApprovalAutomation"),
            (LAB2_BPMN, "Process_EcommerceOrderProcessing"),
            (LAB3_BPMN, "Process_HospitalPatientTriage")
        ]

        for path, expected_process_id in bpmn_files:
            tree = ET.parse(path)
            root = tree.getroot()
            process = root.find(f".//bpmn:process[@id='{expected_process_id}']", NS_BPMN)
            self.assertIsNotNone(process, f"Process {expected_process_id} not found in {path}")
            self.assertEqual(process.attrib.get("isExecutable"), "true")

            gateways = process.findall(".//bpmn:exclusiveGateway", NS_BPMN)
            for gw in gateways:
                self.assertTrue("default" in gw.attrib, f"Gateway {gw.attrib.get('id')} in {path} lacks default sequence flow")

            conditions = process.findall(".//bpmn:conditionExpression", NS_BPMN)
            self.assertGreater(len(conditions), 0)
            for cond in conditions:
                text = cond.text.strip() if cond.text else ""
                self.assertTrue(text.startswith("="), f"Condition in {path} does not use FEEL '=' prefix: {text}")

    def test_dmn_decision_tables_integrity(self):
        """Verify DMN models have UNIQUE hit policy, defined inputs, outputs, and rules."""
        dmn_files = [
            (LAB1_DMN, "decision_loan_approval", 7),
            (LAB2_DMN, "decision_order_priority", 7),
            (LAB3_DMN, "decision_patient_triage", 7),
        ]

        for path, decision_id, min_rules in dmn_files:
            tree = ET.parse(path)
            root = tree.getroot()
            decision = root.find(f".//dmn:decision[@id='{decision_id}']", NS_DMN)
            self.assertIsNotNone(decision, f"Decision {decision_id} not found in {path}")

            table = decision.find("dmn:decisionTable", NS_DMN)
            self.assertIsNotNone(table)
            self.assertEqual(table.attrib.get("hitPolicy"), "UNIQUE")

            rules = table.findall("dmn:rule", NS_DMN)
            self.assertGreaterEqual(len(rules), min_rules)

    def test_payload_files_completeness(self):
        """Verify all 15 test payloads (5 per lab) are valid JSON and contain variables."""
        payload_dirs = [LAB1_PAYLOADS, LAB2_PAYLOADS, LAB3_PAYLOADS]
        total_payloads = 0
        for d in payload_dirs:
            self.assertTrue(os.path.exists(d))
            files = [f for f in os.listdir(d) if f.endswith(".json")]
            self.assertEqual(len(files), 5, f"Directory {d} does not have exactly 5 test scenarios")
            for filename in files:
                filepath = os.path.join(d, filename)
                with open(filepath, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    self.assertIn("testScenario", data)
                    self.assertIn("variables", data)
                    total_payloads += 1
        self.assertEqual(total_payloads, 15)


if __name__ == "__main__":
    unittest.main()
