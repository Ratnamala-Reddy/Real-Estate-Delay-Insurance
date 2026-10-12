import { createFeatureSelector, createSelector } from '@ngrx/store';
import { ApplicationState } from './application.models';

export const selectApplicationState = createFeatureSelector<ApplicationState>('applications');

export const selectAllApplications = createSelector(
  selectApplicationState,
  (state) => state?.applications || []
);

export const selectSelectedApplication = createSelector(
  selectApplicationState,
  (state) => state?.selectedApplication || null
);

export const selectApplicationDocuments = createSelector(
  selectApplicationState,
  (state) => state?.documents || []
);

export const selectApplicationLoading = createSelector(
  selectApplicationState,
  (state) => !!state?.loading
);

export const selectApplicationError = createSelector(
  selectApplicationState,
  (state) => state?.error || null
);
