import { createFeatureSelector, createSelector } from '@ngrx/store';
import { PolicyState } from './policy.models';

export const selectPolicyState = createFeatureSelector<PolicyState>('policy');

export const selectAllQuotes = createSelector(
  selectPolicyState,
  (state) => state?.quotes || []
);

export const selectAllPolicies = createSelector(
  selectPolicyState,
  (state) => state?.policies || []
);

export const selectSelectedPolicy = createSelector(
  selectPolicyState,
  (state) => state?.selectedPolicy || null
);

export const selectPolicyLoading = createSelector(
  selectPolicyState,
  (state) => !!state?.loading
);

export const selectPolicyError = createSelector(
  selectPolicyState,
  (state) => state?.error || null
);
