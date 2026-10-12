import { createAction, props } from '@ngrx/store';
import {
  RiskAssessment,
  RiskAssessmentRequest,
  ApproveRiskRequest,
  DeclineRiskRequest
} from './risk.models';

export const calculateRisk = createAction(
  '[Risk] Calculate Risk',
  props<{ request: RiskAssessmentRequest }>()
);
export const calculateRiskSuccess = createAction(
  '[Risk] Calculate Risk Success',
  props<{ assessment: RiskAssessment }>()
);
export const calculateRiskFailure = createAction(
  '[Risk] Calculate Risk Failure',
  props<{ error: string }>()
);

export const loadRiskAssessment = createAction(
  '[Risk] Load Risk Assessment',
  props<{ applicationId: string }>()
);
export const loadRiskAssessmentSuccess = createAction(
  '[Risk] Load Risk Assessment Success',
  props<{ assessment: RiskAssessment }>()
);
export const loadRiskAssessmentFailure = createAction(
  '[Risk] Load Risk Assessment Failure',
  props<{ error: string }>()
);

export const approveRiskAssessment = createAction(
  '[Risk] Approve Risk Assessment',
  props<{ assessmentId: string; request: ApproveRiskRequest }>()
);
export const approveRiskAssessmentSuccess = createAction(
  '[Risk] Approve Risk Assessment Success',
  props<{ assessment: RiskAssessment }>()
);
export const approveRiskAssessmentFailure = createAction(
  '[Risk] Approve Risk Assessment Failure',
  props<{ error: string }>()
);

export const declineRiskAssessment = createAction(
  '[Risk] Decline Risk Assessment',
  props<{ assessmentId: string; request: DeclineRiskRequest }>()
);
export const declineRiskAssessmentSuccess = createAction(
  '[Risk] Decline Risk Assessment Success',
  props<{ assessment: RiskAssessment }>()
);
export const declineRiskAssessmentFailure = createAction(
  '[Risk] Decline Risk Assessment Failure',
  props<{ error: string }>()
);
