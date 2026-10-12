import { createAction, props } from '@ngrx/store';
import {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  UserProfileResponse,
  UpdateProfileRequest
} from './auth.models';

export const login = createAction(
  '[Auth] Login',
  props<{ request: LoginRequest; returnUrl?: string }>()
);

export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ response: LoginResponse; returnUrl?: string }>()
);

export const loginFailure = createAction(
  '[Auth] Login Failure',
  props<{ error: string }>()
);

export const register = createAction(
  '[Auth] Register',
  props<{ request: RegisterRequest }>()
);

export const registerSuccess = createAction(
  '[Auth] Register Success',
  props<{ response: RegisterResponse }>()
);

export const registerFailure = createAction(
  '[Auth] Register Failure',
  props<{ error: string }>()
);

export const loadProfile = createAction('[Auth] Load Profile');

export const loadProfileSuccess = createAction(
  '[Auth] Load Profile Success',
  props<{ profile: UserProfileResponse }>()
);

export const loadProfileFailure = createAction(
  '[Auth] Load Profile Failure',
  props<{ error: string }>()
);

export const updateProfile = createAction(
  '[Auth] Update Profile',
  props<{ request: UpdateProfileRequest }>()
);

export const updateProfileSuccess = createAction(
  '[Auth] Update Profile Success',
  props<{ profile: UserProfileResponse }>()
);

export const updateProfileFailure = createAction(
  '[Auth] Update Profile Failure',
  props<{ error: string }>()
);

export const logout = createAction('[Auth] Logout');
export const logoutSuccess = createAction('[Auth] Logout Success');
