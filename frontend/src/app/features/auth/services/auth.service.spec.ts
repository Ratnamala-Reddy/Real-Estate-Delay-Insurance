import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../../../environments/environment';
import { LoginRequest, RegisterRequest, UserProfileResponse, UpdateProfileRequest } from '../state/auth.models';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should authenticate user and store token/role', () => {
    const mockRequest: LoginRequest = { email: 'buyer@delayguard.com', password: 'Password123!' };
    const mockRes = { message: 'Logged in successfully', role: 'BUYER', token: 'jwt-token-abc' };

    service.login(mockRequest).subscribe((res) => {
      expect(res.role).toBe('BUYER');
      expect(service.getToken()).toBe('jwt-token-abc');
      expect(service.getRole()).toBe('BUYER');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockRequest);
    req.flush(mockRes);
  });

  it('should register a new user successfully', () => {
    const mockRegister: RegisterRequest = {
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@delayguard.com',
      mobileNumber: '9876543210',
      password: 'Password123!'
    };
    const mockRes = { userId: '101', message: 'User registered successfully' };

    service.register(mockRegister).subscribe((res) => {
      expect(res.userId).toBe('101');
      expect(res.message).toContain('User registered');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/auth/register`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockRegister);
    req.flush(mockRes);
  });

  it('should retrieve current user profile', () => {
    const mockProfile: UserProfileResponse = {
      userId: '101',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@delayguard.com',
      mobileNumber: '9876543210',
      role: 'BUYER',
      status: 'ACTIVE'
    };

    service.getCurrentUser().subscribe((profile) => {
      expect(profile.userId).toBe('101');
      expect(profile.firstName).toBe('John');
      expect(profile.role).toBe('BUYER');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/users/me`);
    expect(req.request.method).toBe('GET');
    req.flush(mockProfile);
  });

  it('should update current user profile', () => {
    const mockUpdate: UpdateProfileRequest = {
      firstName: 'Johnny',
      lastName: 'Doe',
      mobileNumber: '9988776655'
    };
    const mockUpdatedProfile: UserProfileResponse = {
      userId: '101',
      firstName: 'Johnny',
      lastName: 'Doe',
      email: 'john@delayguard.com',
      mobileNumber: '9988776655',
      role: 'BUYER',
      status: 'ACTIVE'
    };

    service.updateProfile(mockUpdate).subscribe((res) => {
      expect(res.firstName).toBe('Johnny');
      expect(res.mobileNumber).toBe('9988776655');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/users/me`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(mockUpdate);
    req.flush(mockUpdatedProfile);
  });

  it('should clear stored credentials on logout', () => {
    localStorage.setItem('access_token', 'jwt-token-abc');
    localStorage.setItem('user_role', 'BUYER');

    service.logout().subscribe(() => {
      expect(service.getToken()).toBeNull();
      expect(service.getRole()).toBeNull();
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/auth/logout`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
