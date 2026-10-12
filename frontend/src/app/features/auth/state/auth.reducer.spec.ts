import { authReducer, initialAuthState } from './auth.reducer';
import * as AuthActions from './auth.actions';
import { LoginResponse, UserProfileResponse } from './auth.models';

describe('Auth Reducer', () => {
  const mockLoginResponse: LoginResponse = {
    token: 'jwt-mock-token',
    role: 'BUYER',
    message: 'Login successful'
  };

  const mockProfile: UserProfileResponse = {
    userId: '10',
    email: 'buyer@test.com',
    role: 'BUYER',
    firstName: 'John',
    lastName: 'Doe',
    mobileNumber: '9876543210',
    status: 'ACTIVE'
  };

  it('should set loading to true on login action', () => {
    const state = authReducer(
      initialAuthState,
      AuthActions.login({ request: { email: 'a@b.com', password: '123' } })
    );
    expect(state.loading).toBeTrue();
    expect(state.error).toBeNull();
  });

  it('should update state on loginSuccess', () => {
    const state = authReducer(
      { ...initialAuthState, loading: true },
      AuthActions.loginSuccess({ response: mockLoginResponse })
    );
    expect(state.loading).toBeFalse();
    expect(state.isAuthenticated).toBeTrue();
    expect(state.role).toBe('BUYER');
    expect(state.token).toBe('jwt-mock-token');
  });

  it('should set error on loginFailure', () => {
    const state = authReducer(
      { ...initialAuthState, loading: true },
      AuthActions.loginFailure({ error: 'Invalid email or password' })
    );
    expect(state.loading).toBeFalse();
    expect(state.error).toBe('Invalid email or password');
  });

  it('should update user on loadProfileSuccess', () => {
    const state = authReducer(
      initialAuthState,
      AuthActions.loadProfileSuccess({ profile: mockProfile })
    );
    expect(state.user).toEqual(mockProfile);
    expect(state.role).toBe('BUYER');
  });

  it('should clear state on logoutSuccess', () => {
    const loggedInState = {
      user: mockProfile,
      token: 'jwt-mock-token',
      role: 'BUYER',
      isAuthenticated: true,
      loading: false,
      error: null
    };
    const state = authReducer(loggedInState, AuthActions.logoutSuccess());
    expect(state.isAuthenticated).toBeFalse();
    expect(state.token).toBeNull();
    expect(state.user).toBeNull();
    expect(state.role).toBeNull();
  });
});

