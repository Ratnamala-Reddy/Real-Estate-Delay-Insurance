export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  message: string;
  role: string;
  token?: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  mobileNumber: string;
  password: string;
}

export interface RegisterResponse {
  userId: string;
  message: string;
}

export interface UserProfileResponse {
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  mobileNumber: string;
  role: string;
  status: string;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  mobileNumber: string;
}

export interface UserStatusUpdateRequest {
  status: 'ACTIVE' | 'SUSPENDED' | 'INACTIVE';
  reason?: string;
}

export interface AuthState {
  user: UserProfileResponse | null;
  token: string | null;
  role: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}
