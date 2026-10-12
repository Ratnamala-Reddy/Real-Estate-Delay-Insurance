import { createReducer, on } from '@ngrx/store';
import { AuthState } from './auth.models';
import * as AuthActions from './auth.actions';

const storedToken = localStorage.getItem('access_token');
const storedRole = localStorage.getItem('user_role');

export const initialAuthState: AuthState = {
  user: null,
  token: storedToken,
  role: storedRole,
  isAuthenticated: !!storedToken || !!storedRole,
  loading: false,
  error: null
};

export const authReducer = createReducer(
  initialAuthState,
  on(AuthActions.login, AuthActions.register, AuthActions.loadProfile, AuthActions.updateProfile, (state) => ({
    ...state,
    loading: true,
    error: null
  })),
  on(AuthActions.loginSuccess, (state, { response }) => ({
    ...state,
    loading: false,
    isAuthenticated: true,
    role: response.role,
    token: response.token || state.token,
    error: null
  })),
  on(AuthActions.loginFailure, AuthActions.registerFailure, AuthActions.loadProfileFailure, AuthActions.updateProfileFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),
  on(AuthActions.registerSuccess, (state) => ({
    ...state,
    loading: false,
    error: null
  })),
  on(AuthActions.loadProfileSuccess, AuthActions.updateProfileSuccess, (state, { profile }) => ({
    ...state,
    loading: false,
    user: profile,
    role: profile.role,
    error: null
  })),
  on(AuthActions.logoutSuccess, () => ({
    user: null,
    token: null,
    role: null,
    isAuthenticated: false,
    loading: false,
    error: null
  }))
);
