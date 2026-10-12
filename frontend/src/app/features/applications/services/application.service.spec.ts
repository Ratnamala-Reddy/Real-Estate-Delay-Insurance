import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApplicationService } from './application.service';
import { environment } from '../../../../environments/environment';
import { Application, CreateApplicationRequest } from '../state/application.models';

describe('ApplicationService', () => {
  let service: ApplicationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApplicationService]
    });
    service = TestBed.inject(ApplicationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch buyer applications', () => {
    const mockApps: Application[] = [
      {
        applicationId: 'APP-101',
        userId: 1,
        builderName: 'DLF',
        projectName: 'Park Place',
        unitNumber: 'A-101',
        propertyValue: 7500000,
        purchaseDate: '2025-01-01',
        promisedPossessionDate: '2026-12-01',
        status: 'SUBMITTED'
      }
    ];

    service.getMyApplications().subscribe((apps) => {
      expect(apps.length).toBe(1);
      expect(apps[0].applicationId).toBe('APP-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/applications`);
    expect(req.request.method).toBe('GET');
    req.flush(mockApps);
  });

  it('should create new application', () => {
    const newApp: CreateApplicationRequest = {
      builderName: 'Godrej',
      projectName: 'Avenues',
      unitNumber: 'B-202',
      propertyValue: 6000000,
      purchaseDate: '2025-02-01',
      promisedPossessionDate: '2027-01-01'
    };

    const mockResponse: Application = {
      applicationId: 'APP-102',
      userId: 1,
      ...newApp,
      status: 'DRAFT'
    };

    service.createApplication(newApp).subscribe((res) => {
      expect(res.applicationId).toBe('APP-102');
      expect(res.status).toBe('DRAFT');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/applications`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newApp);
    req.flush(mockResponse);
  });

  it('should get application by ID', () => {
    const mockApp: Application = {
      applicationId: 'APP-101',
      userId: 1,
      builderName: 'DLF',
      projectName: 'Park Place',
      unitNumber: 'A-101',
      propertyValue: 7500000,
      purchaseDate: '2025-01-01',
      promisedPossessionDate: '2026-12-01',
      status: 'SUBMITTED'
    };

    service.getApplicationById('APP-101').subscribe((app) => {
      expect(app.applicationId).toBe('APP-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/applications/APP-101`);
    expect(req.request.method).toBe('GET');
    req.flush(mockApp);
  });

  it('should submit application', () => {
    const mockApp: Application = {
      applicationId: 'APP-101',
      userId: 1,
      builderName: 'DLF',
      projectName: 'Park Place',
      unitNumber: 'A-101',
      propertyValue: 7500000,
      purchaseDate: '2025-01-01',
      promisedPossessionDate: '2026-12-01',
      status: 'SUBMITTED'
    };

    service.submitApplication('APP-101').subscribe((app) => {
      expect(app.status).toBe('SUBMITTED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/applications/APP-101/submit`);
    expect(req.request.method).toBe('POST');
    req.flush(mockApp);
  });

  it('should withdraw application', () => {
    const mockApp: Application = {
      applicationId: 'APP-101',
      userId: 1,
      builderName: 'DLF',
      projectName: 'Park Place',
      unitNumber: 'A-101',
      propertyValue: 7500000,
      purchaseDate: '2025-01-01',
      promisedPossessionDate: '2026-12-01',
      status: 'WITHDRAWN'
    };

    service.withdrawApplication('APP-101').subscribe((app) => {
      expect(app.status).toBe('WITHDRAWN');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/applications/APP-101/withdraw`);
    expect(req.request.method).toBe('POST');
    req.flush(mockApp);
  });
});
