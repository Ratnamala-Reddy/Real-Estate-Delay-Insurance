import { createAction, props } from '@ngrx/store';
import { UserProfileResponse, UserStatusUpdateRequest } from '../../auth/state/auth.models';

export const loadAllUsers = createAction('[Admin] Load All Users');
export const loadAllUsersSuccess = createAction(
  '[Admin] Load All Users Success',
  props<{ users: UserProfileResponse[] }>()
);
export const loadAllUsersFailure = createAction(
  '[Admin] Load All Users Failure',
  props<{ error: string }>()
);

export const updateUserStatus = createAction(
  '[Admin] Update User Status',
  props<{ userId: number; request: UserStatusUpdateRequest }>()
);
export const updateUserStatusSuccess = createAction(
  '[Admin] Update User Status Success',
  props<{ userId: number; status: string }>()
);
export const updateUserStatusFailure = createAction(
  '[Admin] Update User Status Failure',
  props<{ error: string }>()
);
