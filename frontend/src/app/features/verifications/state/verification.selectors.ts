import { createFeatureSelector, createSelector } from '@ngrx/store';
import { VerificationState } from './verification.models';

export const selectVerificationState = createFeatureSelector<VerificationState>('verifications');

export const selectVerificationQueue = createSelector(
  selectVerificationState,
  (state) => state?.queue || []
);

export const selectCurrentVerification = createSelector(
  selectVerificationState,
  (state) => state?.currentRecord || null
);

export const selectVerificationLoading = createSelector(
  selectVerificationState,
  (state) => !!state?.loading
);

export const selectVerificationError = createSelector(
  selectVerificationState,
  (state) => state?.error || null
);
