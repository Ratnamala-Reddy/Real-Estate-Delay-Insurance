import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ClaimState } from './claim.models';

export const selectClaimState = createFeatureSelector<ClaimState>('claims');

export const selectAllClaims = createSelector(
  selectClaimState,
  (state) => state?.claims || []
);

export const selectSelectedClaim = createSelector(
  selectClaimState,
  (state) => state?.selectedClaim || null
);

export const selectClaimLoading = createSelector(
  selectClaimState,
  (state) => !!state?.loading
);

export const selectClaimError = createSelector(
  selectClaimState,
  (state) => state?.error || null
);
