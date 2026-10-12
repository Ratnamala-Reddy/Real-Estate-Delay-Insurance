import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ClaimService } from './claim.service';
import { environment } from '../../../../environments/environment';
import { Claim, CreateClaimRequest } from '../state/claim.models';

describe('ClaimService', () => {
  let service: ClaimService;
  let httpMock: HttpTestingController;

  const mockClaim: Claim = {
    claimId: 'CLM-101',
    policyId: 'POL-101',
    applicationId: 'APP-101',
    reason: 'Builder delayed handover past promised date',
    claimedAmount: 250000,
    approvedAmount: 200000,
    eligibleDelayMonths: 4,
    status: 'DOCUMENTS_SUBMITTED',
    remarks: 'Documents verified',
    createdAt: '2026-01-01T10:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ClaimService]
    });
    service = TestBed.inject(ClaimService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch claims list', () => {
    service.getClaims().subscribe((claims) => {
      expect(claims.length).toBe(1);
      expect(claims[0].claimId).toBe('CLM-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/claims`);
    expect(req.request.method).toBe('GET');
    req.flush([mockClaim]);
  });

  it('should get claim by id', () => {
    service.getClaimById('CLM-101').subscribe((claim) => {
      expect(claim.claimId).toBe('CLM-101');
      expect(claim.status).toBe('DOCUMENTS_SUBMITTED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/claims/CLM-101`);
    expect(req.request.method).toBe('GET');
    req.flush(mockClaim);
  });

  it('should throw error when getClaimById called with empty id', () => {
    service.getClaimById('').subscribe({
      next: () => fail('Should have failed with empty id'),
      error: (err) => {
        expect(err.message).toContain('Claim ID is required');
      }
    });
  });

  it('should create a new claim', () => {
    const payload: CreateClaimRequest = {
      policyId: 'POL-101',
      reason: 'Delay in possession',
      claimedAmount: 250000
    };

    service.createClaim(payload).subscribe((claim) => {
      expect(claim.claimId).toBe('CLM-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/claims`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockClaim);
  });

  it('should calculate delay for claim', () => {
    const updatedClaim: Claim = { ...mockClaim, status: 'DELAY_VERIFIED', eligibleDelayMonths: 6 };

    service.calculateDelay('CLM-101').subscribe((claim) => {
      expect(claim.status).toBe('DELAY_VERIFIED');
      expect(claim.eligibleDelayMonths).toBe(6);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/claims/CLM-101/delay-calculation`);
    expect(req.request.method).toBe('POST');
    req.flush(updatedClaim);
  });

  it('should calculate compensation for claim', () => {
    const updatedClaim: Claim = { ...mockClaim, status: 'ELIGIBILITY_VERIFIED', approvedAmount: 220000 };

    service.calculateCompensation('CLM-101').subscribe((claim) => {
      expect(claim.approvedAmount).toBe(220000);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/claims/CLM-101/compensation-calculation`);
    expect(req.request.method).toBe('POST');
    req.flush(updatedClaim);
  });

  it('should verify insurance eligibility', () => {
    const updatedClaim: Claim = { ...mockClaim, status: 'ELIGIBILITY_VERIFIED' };

    service.verifyInsuranceEligibility('CLM-101').subscribe((claim) => {
      expect(claim.status).toBe('ELIGIBILITY_VERIFIED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/claims/CLM-101/insurance-verification`);
    expect(req.request.method).toBe('POST');
    req.flush(updatedClaim);
  });

  it('should approve claim', () => {
    const updatedClaim: Claim = { ...mockClaim, status: 'APPROVED' };

    service.approveClaim('CLM-101').subscribe((claim) => {
      expect(claim.status).toBe('APPROVED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/claims/CLM-101/approve`);
    expect(req.request.method).toBe('POST');
    req.flush(updatedClaim);
  });

  it('should reject claim', () => {
    const updatedClaim: Claim = { ...mockClaim, status: 'REJECTED', remarks: 'Not eligible under policy terms' };

    service.rejectClaim('CLM-101').subscribe((claim) => {
      expect(claim.status).toBe('REJECTED');
      expect(claim.remarks).toContain('Not eligible');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/claims/CLM-101/reject`);
    expect(req.request.method).toBe('POST');
    req.flush(updatedClaim);
  });

  it('should close claim', () => {
    const updatedClaim: Claim = { ...mockClaim, status: 'CLOSED' };

    service.closeClaim('CLM-101').subscribe((claim) => {
      expect(claim.status).toBe('CLOSED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/claims/CLM-101/close`);
    expect(req.request.method).toBe('POST');
    req.flush(updatedClaim);
  });
});
