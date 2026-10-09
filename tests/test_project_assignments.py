import os
import unittest
import xml.etree.ElementTree as ET

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROJECT_ASSIGNMENTS_DIR = os.path.join(BASE_DIR, "Project_Assignments")

class TestProjectAssignments(unittest.TestCase):

    def setUp(self):
        self.a1_bpmn = os.path.join(PROJECT_ASSIGNMENTS_DIR, "Assignment_1_Student_Project_Allocation", "Student_Project_Approval_and_Allocation.bpmn")
        self.a1_report = os.path.join(PROJECT_ASSIGNMENTS_DIR, "Assignment_1_Student_Project_Allocation", "Assignment_1_Report.md")
        self.a3_bpmn = os.path.join(PROJECT_ASSIGNMENTS_DIR, "Assignment_3_Loan_Origination_Approval", "Loan_Origination_and_Approval_System.bpmn")
        self.a3_report = os.path.join(PROJECT_ASSIGNMENTS_DIR, "Assignment_3_Loan_Origination_Approval", "Assignment_3_Report.md")

    def test_files_exist(self):
        self.assertTrue(os.path.isfile(self.a1_bpmn), "Assignment 1 BPMN file missing")
        self.assertTrue(os.path.isfile(self.a1_report), "Assignment 1 Report missing")
        self.assertTrue(os.path.isfile(self.a3_bpmn), "Assignment 3 BPMN file missing")
        self.assertTrue(os.path.isfile(self.a3_report), "Assignment 3 Report missing")

    def test_assignment1_structure_and_lanes(self):
        tree = ET.parse(self.a1_bpmn)
        root = tree.getroot()
        ns = {'bpmn': 'http://www.omg.org/spec/BPMN/20100524/MODEL'}

        # Verify Participant
        participant = root.find('.//bpmn:participant[@id="Participant_Department"]', ns)
        self.assertIsNotNone(participant, "Participant_Department pool missing")

        # Verify Lanes
        lane_ids = ['Lane_Student', 'Lane_Coordinator', 'Lane_Committee', 'Lane_Guide', 'Lane_System']
        for lane_id in lane_ids:
            lane = root.find(f'.//bpmn:lane[@id="{lane_id}"]', ns)
            self.assertIsNotNone(lane, f"Lane {lane_id} missing in Assignment 1")

        # Verify all Failure Paths F1 to F11 are present
        f_nodes = [
            'Activity_ReturnErrorList',         # F1
            'Event_End_InvalidTeam',            # F2
            'Activity_ReviewDuplicate',         # F3
            'Event_Timer_ProposalDeadline',     # F4
            'Activity_ReviseProposal',          # F5
            'Activity_SendRejectionReasons',    # F6
            'Event_Timer_ReviewReminder',       # F7
            'Activity_ManualGuideAllocation',   # F8
            'Event_Error_GuideDecline',         # F9
            'Event_Error_NotificationFailure',  # F10
            'Event_Message_Withdrawal',         # F11
            'Activity_ReleaseSlot'              # F11 Compensation
        ]
        for node_id in f_nodes:
            node = root.find(f'.//*[@id="{node_id}"]', ns)
            self.assertIsNotNone(node, f"Failure node {node_id} missing in Assignment 1")

    def test_assignment1_flow_connectivity(self):
        tree = ET.parse(self.a1_bpmn)
        root = tree.getroot()
        ns = {
            'bpmn': 'http://www.omg.org/spec/BPMN/20100524/MODEL',
            'bpmndi': 'http://www.omg.org/spec/BPMN/20100524/DI'
        }

        # Collect all element IDs in process and subprocess
        element_ids = set()
        for elem in root.iter():
            eid = elem.attrib.get('id')
            if eid:
                element_ids.add(eid)

        # Verify all sequence flows connect existing elements
        flows = root.findall('.//bpmn:sequenceFlow', ns)
        self.assertGreater(len(flows), 30, "Expected at least 30 sequence flows in Assignment 1")
        for flow in flows:
            fid = flow.attrib.get('id')
            source = flow.attrib.get('sourceRef')
            target = flow.attrib.get('targetRef')
            self.assertIn(source, element_ids, f"Flow {fid} has invalid sourceRef {source}")
            self.assertIn(target, element_ids, f"Flow {fid} has invalid targetRef {target}")

        # Verify BPMNDI Shapes and Edges exist
        shapes = root.findall('.//bpmndi:BPMNShape', ns)
        edges = root.findall('.//bpmndi:BPMNEdge', ns)
        self.assertGreater(len(shapes), 25, "Expected BPMNDI shapes in Assignment 1")
        self.assertGreater(len(edges), 30, "Expected BPMNDI edges in Assignment 1")

    def test_assignment3_structure_and_lanes(self):
        tree = ET.parse(self.a3_bpmn)
        root = tree.getroot()
        ns = {'bpmn': 'http://www.omg.org/spec/BPMN/20100524/MODEL'}

        # Verify External Pool and Bank Pool
        ext_pool = root.find('.//bpmn:participant[@id="Participant_CreditBureau"]', ns)
        bank_pool = root.find('.//bpmn:participant[@id="Participant_Bank"]', ns)
        self.assertIsNotNone(ext_pool, "External Credit Bureau pool missing")
        self.assertIsNotNone(bank_pool, "Main Bank pool missing")

        # Verify Message Flows between External Pool and Core Banking
        msg_flows = root.findall('.//bpmn:messageFlow', ns)
        self.assertEqual(len(msg_flows), 2, "Expected 2 message flows with Credit Bureau")

        # Verify all 6 Lanes in Bank
        lane_ids = ['Lane_Applicant', 'Lane_LoanOfficer', 'Lane_Underwriter', 'Lane_Compliance', 'Lane_Operations', 'Lane_CoreBanking']
        for lane_id in lane_ids:
            lane = root.find(f'.//bpmn:lane[@id="{lane_id}"]', ns)
            self.assertIsNotNone(lane, f"Lane {lane_id} missing in Assignment 3")

        # Verify all Failure Paths L1 to L12 are present
        l_nodes = [
            'Activity_RequestMissingDocs',       # L1
            'Activity_ManualKYCResolution',     # L2
            'SubProcess_FraudInvestigation',    # L3
            'Event_End_TerminatedFraud',        # L3 Terminate
            'Event_Error_BureauAPI',            # L4
            'Activity_PrepareCounterOffer',     # L5, L6
            'Activity_SeniorCommitteeApproval', # L7
            'Activity_CollateralValuation',     # L8
            'Event_Timer_OfferExpiry',          # L9
            'Event_Message_Withdrawal',         # L10
            'Event_Throw_WithdrawCompensation', # L10
            'Gateway_ESignAttempts',            # L11
            'Event_Error_DisbursementFail',     # L12
            'Event_Throw_CompensateAccount',    # L12
            'Activity_ReverseLoanAccount'       # L12 Compensation handler
        ]
        for node_id in l_nodes:
            node = root.find(f'.//*[@id="{node_id}"]', ns)
            self.assertIsNotNone(node, f"Failure node {node_id} missing in Assignment 3")

    def test_assignment3_flow_connectivity(self):
        tree = ET.parse(self.a3_bpmn)
        root = tree.getroot()
        ns = {
            'bpmn': 'http://www.omg.org/spec/BPMN/20100524/MODEL',
            'bpmndi': 'http://www.omg.org/spec/BPMN/20100524/DI'
        }

        element_ids = set()
        for elem in root.iter():
            eid = elem.attrib.get('id')
            if eid:
                element_ids.add(eid)

        flows = root.findall('.//bpmn:sequenceFlow', ns)
        self.assertGreater(len(flows), 35, "Expected at least 35 sequence flows in Assignment 3")
        for flow in flows:
            fid = flow.attrib.get('id')
            source = flow.attrib.get('sourceRef')
            target = flow.attrib.get('targetRef')
            self.assertIn(source, element_ids, f"Flow {fid} has invalid sourceRef {source}")
            self.assertIn(target, element_ids, f"Flow {fid} has invalid targetRef {target}")

        shapes = root.findall('.//bpmndi:BPMNShape', ns)
        edges = root.findall('.//bpmndi:BPMNEdge', ns)
        self.assertGreater(len(shapes), 30, "Expected BPMNDI shapes in Assignment 3")
        self.assertGreater(len(edges), 35, "Expected BPMNDI edges in Assignment 3")

if __name__ == '__main__':
    unittest.main()
