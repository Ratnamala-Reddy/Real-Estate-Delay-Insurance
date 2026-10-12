import { createFeatureSelector, createSelector } from '@ngrx/store';
import { RiskState } from './risk.models';

export const selectRiskState = createFeatureSelector<RiskState>('risk');

export const selectCurrentRiskAssessment = createSelector(
  selectRiskState,
  (state) => state?.currentAssessment || null
);

export const selectRiskLoading = createSelector(
  selectRiskState,
  (state) => !!state?.loading
);

export const selectRiskError = createSelector(
  selectRiskState,
  (state) => state?.error || null
);
