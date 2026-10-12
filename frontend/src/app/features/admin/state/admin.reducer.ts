import { createReducer, on } from '@ngrx/store';
import { AdminState } from './admin.models';
import * as AdminActions from './admin.actions';

export const initialAdminState: AdminState = {
  users: [],
  loading: false,
  error: null
};

export const adminReducer = createReducer(
  initialAdminState,
  on(AdminActions.loadAllUsers, AdminActions.updateUserStatus, (state) => ({
    ...state,
    loading: true,
    error: null
  })),
  on(AdminActions.loadAllUsersSuccess, (state, { users }) => ({
    ...state,
    loading: false,
    users,
    error: null
  })),
  on(AdminActions.updateUserStatusSuccess, (state, { userId, status }) => ({
    ...state,
    loading: false,
    users: state.users.map((u) => (Number(u.userId) === userId ? { ...u, status } : u)),
    error: null
  })),
  on(AdminActions.loadAllUsersFailure, AdminActions.updateUserStatusFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  }))
);
