import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { VerificationService } from './verification.service';
import { environment } from '../../../../environments/environment';
import {
  ApplicationQueueItem,
  VerificationRecord,
  PropertyVerificationRequest,
  AdditionalInfoRequest,
  ApprovalRequest,
  RejectVerificationRequest
} from '../state/verification.models';

describe('VerificationService', () => {
  let service: VerificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [VerificationService]
    });
    service = TestBed.inject(VerificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch verification queue', () => {
    const mockQueue: ApplicationQueueItem[] = [
      {
        applicationId: 'APP-101',
        userId: 1,
        builderName: 'DLF',
        projectName: 'Park Place',
        unitNumber: 'A-101',
        propertyValue: 5000000,
        purchaseDate: '2025-01-01',
        promisedPossessionDate: '2026-06-01',
        status: 'SUBMITTED'
      }
    ];

    service.getVerificationQueue().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].builderName).toBe('DLF');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/verifications/applications`);
    expect(req.request.method).toBe('GET');
    req.flush(mockQueue);
  });

  it('should get verification result by application id', () => {
    const mockRecord: VerificationRecord = {
      verificationId: 'VER-101',
      applicationId: 'APP-101',
      verificationStatus: 'VERIFIED',
      remarks: 'All documents verified successfully',
      verifiedBy: 'underwriter@gmail.com',
      verifiedAt: '2026-01-01T10:00:00Z'
    };

    service.getVerificationResult('APP-101').subscribe((res) => {
      expect(res.verificationId).toBe('VER-101');
      expect(res.verificationStatus).toBe('VERIFIED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/verifications/applications/APP-101`);
    expect(req.request.method).toBe('GET');
    req.flush(mockRecord);
  });

  it('should verify property', () => {
    const payload: PropertyVerificationRequest = {
      remarks: 'Property RERA verified',
      verifiedBy: 'underwriter@gmail.com'
    };
    const mockRecord: VerificationRecord = {
      verificationId: 'VER-101',
      applicationId: 'APP-101',
      verificationStatus: 'PROPERTY_VERIFIED',
      remarks: 'Property RERA verified',
      verifiedBy: 'underwriter@gmail.com',
      verifiedAt: '2026-01-01T10:00:00Z'
    };

    service.verifyProperty('APP-101', payload).subscribe((res) => {
      expect(res.verificationStatus).toBe('PROPERTY_VERIFIED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/verifications/applications/APP-101/property-verification`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockRecord);
  });

  it('should request additional information', () => {
    const payload: AdditionalInfoRequest = {
      remarks: 'Need clear NOC copy',
      requestedBy: 'underwriter@gmail.com'
    };
    const mockRecord: VerificationRecord = {
      verificationId: 'VER-101',
      applicationId: 'APP-101',
      verificationStatus: 'ADDITIONAL_INFORMATION_REQUIRED',
      remarks: 'Need clear NOC copy',
      verifiedBy: 'underwriter@gmail.com',
      verifiedAt: '2026-01-01T10:00:00Z'
    };

    service.requestInformation('APP-101', payload).subscribe((res) => {
      expect(res.verificationStatus).toBe('ADDITIONAL_INFORMATION_REQUIRED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/verifications/applications/APP-101/request-information`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockRecord);
  });

  it('should approve verification', () => {
    const payload: ApprovalRequest = {
      remarks: 'Approved completely',
      approvedBy: 'underwriter@gmail.com'
    };
    const mockRecord: VerificationRecord = {
      verificationId: 'VER-101',
      applicationId: 'APP-101',
      verificationStatus: 'VERIFIED',
      remarks: 'Approved completely',
      verifiedBy: 'underwriter@gmail.com',
      verifiedAt: '2026-01-01T10:00:00Z'
    };

    service.approveVerification('APP-101', payload).subscribe((res) => {
      expect(res.verificationStatus).toBe('VERIFIED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/verifications/applications/APP-101/approve`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockRecord);
  });

  it('should reject application', () => {
    const payload: RejectVerificationRequest = {
      remarks: 'Disputed title documents',
      rejectedBy: 'underwriter@gmail.com'
    };
    const mockRecord: VerificationRecord = {
      verificationId: 'VER-101',
      applicationId: 'APP-101',
      verificationStatus: 'REJECTED',
      remarks: 'Disputed title documents',
      verifiedBy: 'underwriter@gmail.com',
      verifiedAt: '2026-01-01T10:00:00Z'
    };

    service.rejectApplication('APP-101', payload).subscribe((res) => {
      expect(res.verificationStatus).toBe('REJECTED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/verifications/applications/APP-101/reject`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockRecord);
  });
});
