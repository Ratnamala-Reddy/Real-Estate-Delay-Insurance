import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { AuthInterceptor } from './auth.interceptor';

describe('AuthInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
      ]
    });
    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
    localStorage.setItem('access_token', 'mock_token_123');
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should attach Authorization header and withCredentials flag', () => {
    httpClient.get('/api/test').subscribe();
    const req = httpMock.expectOne('/api/test');
    expect(req.request.headers.get('Authorization')).toBe('Bearer mock_token_123');
    expect(req.request.withCredentials).toBeTrue();
    req.flush({});
  });
});
