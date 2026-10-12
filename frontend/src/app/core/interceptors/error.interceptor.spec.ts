import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ErrorInterceptor } from './error.interceptor';
import { NotificationService } from '../services/notification.service';

describe('ErrorInterceptor', () => {
  let httpMock: HttpTestingController;
  let httpClient: HttpClient;
  let routerSpy = { navigate: jasmine.createSpy('navigate') };
  let notificationSpy = { error: jasmine.createSpy('error'), warning: jasmine.createSpy('warning') };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
        { provide: Router, useValue: routerSpy },
        { provide: NotificationService, useValue: notificationSpy }
      ]
    });
    httpMock = TestBed.inject(HttpTestingController);
    httpClient = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem('token');
  });

  it('should catch 401 and redirect to login when token exists', () => {
    localStorage.setItem('token', 'valid-mock-token');
    httpClient.get('/api/test').subscribe({
      error: () => {}
    });
    const req = httpMock.expectOne('/api/test');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    expect(notificationSpy.error).toHaveBeenCalled();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
