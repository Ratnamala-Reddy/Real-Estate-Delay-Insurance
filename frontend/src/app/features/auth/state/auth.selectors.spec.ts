import { AuthState, UserProfileResponse } from './auth.models';
import {
  selectIsAuthenticated,
  selectCurrentUser,
  selectUserRole,
  selectAuthLoading,
  selectAuthError
} from './auth.selectors';

describe('Auth Selectors', () => {
  const mockUser: UserProfileResponse = {
    userId: '1',
    email: 'admin@test.com',
    role: 'ADMIN',
    firstName: 'Admin',
    lastName: 'User',
    mobileNumber: '9988776655',
    status: 'ACTIVE'
  };

  const state: AuthState = {
    user: mockUser,
    token: 'jwt-123',
    role: 'ADMIN',
    isAuthenticated: true,
    loading: false,
    error: null
  };

  it('selectIsAuthenticated should return authentication flag', () => {
    expect(selectIsAuthenticated.projector(state)).toBeTrue();
  });

  it('selectCurrentUser should return user profile', () => {
    expect(selectCurrentUser.projector(state)).toEqual(mockUser);
  });

  it('selectUserRole should return role', () => {
    expect(selectUserRole.projector(state)).toBe('ADMIN');
  });

  it('selectAuthLoading should return loading status', () => {
    expect(selectAuthLoading.projector(state)).toBeFalse();
  });

  it('selectAuthError should return error', () => {
    expect(selectAuthError.projector(state)).toBeNull();
  });
});

