import { UserProfileResponse, UserStatusUpdateRequest } from '../../auth/state/auth.models';

export interface AdminState {
  users: UserProfileResponse[];
  loading: boolean;
  error: string | null;
}
