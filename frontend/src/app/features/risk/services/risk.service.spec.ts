import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RiskService } from './risk.service';
import { environment } from '../../../../environments/environment';
import {
  RiskAssessment,
  RiskAssessmentRequest,
  ApproveRiskRequest,
  DeclineRiskRequest
} from '../state/risk.models';

describe('RiskService', () => {
  let service: RiskService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RiskService]
    });
    service = TestBed.inject(RiskService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should calculate risk assessment', () => {
    const request: RiskAssessmentRequest = {
      applicationId: 'APP-101',
      assessedBy: 'underwriter@gmail.com',
      builderScore: 85,
      financialScore: 80,
      projectProgressScore: 75,
      locationScore: 90,
      approvalScore: 85
    };

    const mockResponse: RiskAssessment = {
      assessmentId: 'RISK-101',
      applicationId: 'APP-101',
      riskScore: 25,
      riskCategory: 'LOW',
      premiumMultiplier: 1.0,
      remarks: 'Low risk project',
      status: 'PENDING',
      assessedBy: 'underwriter@gmail.com',
      assessedAt: '2026-01-01T10:00:00Z'
    };

    service.calculateRisk(request).subscribe((res) => {
      expect(res.assessmentId).toBe('RISK-101');
      expect(res.riskCategory).toBe('LOW');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/risk-assessments`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should get risk assessment by application id', () => {
    const mockResponse: RiskAssessment = {
      assessmentId: 'RISK-101',
      applicationId: 'APP-101',
      riskScore: 25,
      riskCategory: 'LOW',
      premiumMultiplier: 1.0,
      remarks: 'Low risk project',
      status: 'APPROVED',
      assessedBy: 'underwriter@gmail.com',
      assessedAt: '2026-01-01T10:00:00Z'
    };

    service.getRiskAssessment('APP-101').subscribe((res) => {
      expect(res.applicationId).toBe('APP-101');
      expect(res.status).toBe('APPROVED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/risk-assessments/applications/APP-101/risk-assessment`);
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should approve risk assessment', () => {
    const request: ApproveRiskRequest = {
      remarks: 'Approved after verification',
      approvedBy: 'underwriter@gmail.com'
    };
    const mockResponse: RiskAssessment = {
      assessmentId: 'RISK-101',
      applicationId: 'APP-101',
      riskScore: 25,
      riskCategory: 'LOW',
      premiumMultiplier: 1.0,
      remarks: 'Approved after verification',
      status: 'APPROVED',
      assessedBy: 'underwriter@gmail.com',
      assessedAt: '2026-01-01T10:00:00Z'
    };

    service.approveRiskAssessment('RISK-101', request).subscribe((res) => {
      expect(res.status).toBe('APPROVED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/risk-assessments/RISK-101/approve`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });

  it('should decline risk assessment', () => {
    const request: DeclineRiskRequest = {
      remarks: 'High risk of default',
      declinedBy: 'underwriter@gmail.com'
    };
    const mockResponse: RiskAssessment = {
      assessmentId: 'RISK-101',
      applicationId: 'APP-101',
      riskScore: 85,
      riskCategory: 'HIGH',
      premiumMultiplier: 2.0,
      remarks: 'High risk of default',
      status: 'DECLINED',
      assessedBy: 'underwriter@gmail.com',
      assessedAt: '2026-01-01T10:00:00Z'
    };

    service.declineRiskAssessment('RISK-101', request).subscribe((res) => {
      expect(res.status).toBe('DECLINED');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/risk-assessments/RISK-101/decline`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockResponse);
  });
});
