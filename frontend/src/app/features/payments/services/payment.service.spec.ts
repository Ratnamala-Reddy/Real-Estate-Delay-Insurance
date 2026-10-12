import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PaymentService } from './payment.service';
import { environment } from '../../../../environments/environment';
import {
  PremiumPayment,
  PremiumPaymentRequest,
  CompensationPayment,
  CompensationPaymentRequest
} from '../state/payment.models';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  const mockPremium: PremiumPayment = {
    paymentId: 'PAY-101',
    quoteId: 'QTE-101',
    applicationId: 'APP-101',
    premiumAmount: 18500,
    paymentProvider: 'RAZORPAY',
    transactionReference: 'TXN-ABC-123',
    status: 'SUCCESS',
    createdAt: '2026-01-01T10:00:00Z'
  };

  const mockCompensation: CompensationPayment = {
    paymentId: 'CMP-101',
    claimId: 'CLM-101',
    policyId: 'POL-101',
    payoutAmount: 200000,
    transactionReference: 'CMP-TXN-999',
    status: 'PAID',
    createdAt: '2026-01-02T10:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PaymentService]
    });
    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should create premium payment', () => {
    const request: PremiumPaymentRequest = {
      quoteId: 'QTE-101',
      paymentProvider: 'RAZORPAY'
    };

    service.createPremiumPayment(request).subscribe((res) => {
      expect(res.paymentId).toBe('PAY-101');
      expect(res.premiumAmount).toBe(18500);
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/premium-payments`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockPremium);
  });

  it('should get all premium payments', () => {
    service.getAllPremiumPayments().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].paymentId).toBe('PAY-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/premium-payments`);
    expect(req.request.method).toBe('GET');
    req.flush([mockPremium]);
  });

  it('should get premium payment by ID', () => {
    service.getPremiumPayment('PAY-101').subscribe((res) => {
      expect(res.paymentId).toBe('PAY-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/premium-payments/PAY-101`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPremium);
  });

  it('should create compensation payment', () => {
    const request: CompensationPaymentRequest = {
      claimId: 'CLM-101',
      policyId: 'POL-101',
      payoutAmount: 200000
    };

    service.createCompensationPayment(request).subscribe((res) => {
      expect(res.paymentId).toBe('CMP-101');
      expect(res.status).toBe('PAID');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/compensation-payments`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockCompensation);
  });

  it('should get all compensation payments', () => {
    service.getAllCompensationPayments().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].paymentId).toBe('CMP-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/compensation-payments`);
    expect(req.request.method).toBe('GET');
    req.flush([mockCompensation]);
  });

  it('should get compensation payment by ID', () => {
    service.getCompensationPayment('CMP-101').subscribe((res) => {
      expect(res.paymentId).toBe('CMP-101');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/compensation-payments/CMP-101`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCompensation);
  });

  it('should process premium webhook', () => {
    const webhookReq = {
      paymentId: 'PAY-101',
      transactionReference: 'TXN-ABC-123',
      paymentStatus: 'SUCCESS'
    };

    service.processPremiumWebhook(webhookReq).subscribe((res) => {
      expect(res.status).toBe('SUCCESS');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/payment-webhooks/premium`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(webhookReq);
    req.flush(mockPremium);
  });

  it('should process compensation webhook', () => {
    const webhookReq = {
      paymentId: 'CMP-101',
      transactionReference: 'CMP-TXN-999',
      paymentStatus: 'PAID'
    };

    service.processCompensationWebhook(webhookReq).subscribe((res) => {
      expect(res.status).toBe('PAID');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/payment-webhooks/compensation`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(webhookReq);
    req.flush(mockCompensation);
  });
});
