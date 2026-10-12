import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AuthState } from './auth.models';

export const selectAuthState = createFeatureSelector<AuthState>('auth');

export const selectIsAuthenticated = createSelector(
  selectAuthState,
  (state) => !!state?.isAuthenticated
);

export const selectCurrentUser = createSelector(
  selectAuthState,
  (state) => state?.user || null
);

export const selectUserRole = createSelector(
  selectAuthState,
  (state) => state?.role || (typeof localStorage !== 'undefined' ? localStorage.getItem('user_role') : null)
);

export const selectAuthLoading = createSelector(
  selectAuthState,
  (state) => !!state?.loading
);

export const selectAuthError = createSelector(
  selectAuthState,
  (state) => state?.error || null
);
