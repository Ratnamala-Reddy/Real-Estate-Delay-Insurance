export type RiskCategory = 'LOW' | 'MEDIUM' | 'HIGH';
export type RiskAssessmentStatus = 'PENDING' | 'APPROVED' | 'DECLINED';

export interface RiskAssessmentRequest {
  applicationId: string;
  assessedBy: string;
  builderScore: number;
  financialScore: number;
  projectProgressScore: number;
  locationScore: number;
  approvalScore: number;
}

export interface RiskAssessment {
  assessmentId: string;
  applicationId: string;
  riskScore: number;
  riskCategory: RiskCategory;
  premiumMultiplier: number;
  remarks: string;
  status: RiskAssessmentStatus;
  assessedBy: string;
  assessedAt: string;
}

export interface ApproveRiskRequest {
  remarks: string;
  approvedBy: string;
}

export interface DeclineRiskRequest {
  remarks: string;
  declinedBy: string;
}

export interface RiskState {
  currentAssessment: RiskAssessment | null;
  loading: boolean;
  error: string | null;
}
