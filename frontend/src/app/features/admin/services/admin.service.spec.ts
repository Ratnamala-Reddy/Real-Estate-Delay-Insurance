import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AdminService } from './admin.service';
import { environment } from '../../../../environments/environment';

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AdminService]
    });
    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch all users', () => {
    const mockUsers = [{ userId: '1', firstName: 'Admin', lastName: 'User', email: 'admin@test.com', mobileNumber: '9999999999', role: 'ADMIN', status: 'ACTIVE' }];
    service.getAllUsers().subscribe((res) => {
      expect(res.length).toBe(1);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/admin/users`);
    expect(req.request.method).toBe('GET');
    req.flush(mockUsers);
  });
});
