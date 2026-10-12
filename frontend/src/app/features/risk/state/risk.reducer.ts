import { createReducer, on } from '@ngrx/store';
import { RiskState } from './risk.models';
import * as RiskActions from './risk.actions';

export const initialRiskState: RiskState = {
  currentAssessment: null,
  loading: false,
  error: null
};

export const riskReducer = createReducer(
  initialRiskState,
  on(
    RiskActions.calculateRisk,
    RiskActions.loadRiskAssessment,
    RiskActions.approveRiskAssessment,
    RiskActions.declineRiskAssessment,
    (state) => ({ ...state, loading: true, error: null })
  ),
  on(
    RiskActions.calculateRiskSuccess,
    RiskActions.loadRiskAssessmentSuccess,
    RiskActions.approveRiskAssessmentSuccess,
    RiskActions.declineRiskAssessmentSuccess,
    (state, { assessment }) => ({
      ...state,
      loading: false,
      currentAssessment: assessment,
      error: null
    })
  ),
  on(
    RiskActions.calculateRiskFailure,
    RiskActions.loadRiskAssessmentFailure,
    RiskActions.approveRiskAssessmentFailure,
    RiskActions.declineRiskAssessmentFailure,
    (state, { error }) => ({
      ...state,
      loading: false,
      error
    })
  )
);
