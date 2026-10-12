import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PolicyService } from './policy.service';
import { environment } from '../../../../environments/environment';
import { Quote, Policy, GenerateQuoteRequest, CreatePolicyRequest } from '../state/policy.models';

describe('PolicyService', () => {
  let service: PolicyService;
  let httpMock: HttpTestingController;

  const mockQuote: Quote = {
    quoteId: 'QTE-101',
    applicationId: 'APP-101',
    planName: 'STANDARD',
    premiumAmount: 18500,
    monthlyCompensation: 25000,
    maximumDelayMonths: 12,
    maximumPayout: 300000,
    gracePeriodDays: 30,
    expiryDate: '2026-12-31',
    status: 'GENERATED',
    createdAt: '2026-01-01T10:00:00Z'
  };

  const mockPolicy: Policy = {
    policyId: 'POL-101',
    applicationId: 'APP-101',
    quoteId: 'QTE-101',
    paymentId: 'PAY-101',
    policyNumber: 'POL-2026-0001',
    coverageAmount: 5000000,
    premiumAmount: 18500,
    policyStatus: 'ACTIVE',
    startDate: '2026-01-01',
    endDate: '2027-01-01',
    createdAt: '2026-01-01T10:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PolicyService]
    });
    service = TestBed.inject(PolicyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should get quotes for application', () => {
    service.getQuotesByApplication('APP-101').subscribe((quotes) => {
      expect(quotes.length).toBe(1);
      expect(quotes[0].quoteId).toBe('QTE-101');
      expect(quotes[0].planName).toBe('STANDARD');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/quotes/applications/APP-101`);
    expect(req.request.method).toBe('GET');
    req.flush([mockQuote]);
  });

  it('should generate quotes for application', () => {
    const request: GenerateQuoteRequest = {
      applicationId: 'APP-101',
      baseCoverageAmount: 5000000
    };

    service.generateQuotes('APP-101', request).subscribe((quotes) => {
      expect(quotes.length).toBe(1);
      expect(quotes[0].quoteId).toBe('QTE-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/quotes/applications/APP-101`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush([mockQuote]);
  });

  it('should accept quote', () => {
    const acceptedQuote: Quote = { ...mockQuote, status: 'ACCEPTED' };

    service.acceptQuote('QTE-101').subscribe((res) => {
      expect(res.status).toBe('ACCEPTED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/quotes/QTE-101/accept`);
    expect(req.request.method).toBe('POST');
    req.flush(acceptedQuote);
  });

  it('should get quote by ID', () => {
    service.getQuoteById('QTE-101').subscribe((quote) => {
      expect(quote.quoteId).toBe('QTE-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/quotes/QTE-101`);
    expect(req.request.method).toBe('GET');
    req.flush(mockQuote);
  });

  it('should create policy', () => {
    const request: CreatePolicyRequest = {
      quoteId: 'QTE-101',
      paymentId: 'PAY-101'
    };

    service.createPolicy(request).subscribe((policy) => {
      expect(policy.policyId).toBe('POL-101');
      expect(policy.policyStatus).toBe('ACTIVE');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/policies`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockPolicy);
  });

  it('should get policies list', () => {
    service.getPolicies().subscribe((policies) => {
      expect(policies.length).toBe(1);
      expect(policies[0].policyId).toBe('POL-101');
      expect(policies[0].policyStatus).toBe('ACTIVE');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/policies`);
    expect(req.request.method).toBe('GET');
    req.flush([mockPolicy]);
  });

  it('should get policy by ID', () => {
    service.getPolicyById('POL-101').subscribe((policy) => {
      expect(policy.policyId).toBe('POL-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/policies/POL-101`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPolicy);
  });

  it('should complete policy', () => {
    const completedPolicy: Policy = { ...mockPolicy, policyStatus: 'COMPLETED' };

    service.completePolicy('POL-101').subscribe((policy) => {
      expect(policy.policyStatus).toBe('COMPLETED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/policies/POL-101/complete`);
    expect(req.request.method).toBe('POST');
    req.flush(completedPolicy);
  });

  it('should close policy', () => {
    const closedPolicy: Policy = { ...mockPolicy, policyStatus: 'CLOSED' };

    service.closePolicy('POL-101').subscribe((policy) => {
      expect(policy.policyStatus).toBe('CLOSED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/policies/POL-101/close`);
    expect(req.request.method).toBe('POST');
    req.flush(closedPolicy);
  });
});
