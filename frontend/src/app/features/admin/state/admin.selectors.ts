import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AdminState } from './admin.models';

export const selectAdminState = createFeatureSelector<AdminState>('admin');

export const selectAllAdminUsers = createSelector(
  selectAdminState,
  (state) => state?.users || []
);

export const selectAdminLoading = createSelector(
  selectAdminState,
  (state) => !!state?.loading
);

export const selectAdminError = createSelector(
  selectAdminState,
  (state) => state?.error || null
);
