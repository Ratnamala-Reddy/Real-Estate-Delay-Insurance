import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  RiskAssessment,
  RiskAssessmentRequest,
  ApproveRiskRequest,
  DeclineRiskRequest
} from '../state/risk.models';

@Injectable({
  providedIn: 'root'
})
export class RiskService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/risk-assessments`;

  constructor(private http: HttpClient) {}

  calculateRisk(request: RiskAssessmentRequest): Observable<RiskAssessment> {
    return this.http.post<RiskAssessment>(this.baseUrl, request);
  }

  getRiskAssessment(applicationId: string): Observable<RiskAssessment> {
    return this.http.get<RiskAssessment>(`${this.baseUrl}/applications/${applicationId}/risk-assessment`);
  }

  approveRiskAssessment(assessmentId: string, request: ApproveRiskRequest): Observable<RiskAssessment> {
    return this.http.post<RiskAssessment>(`${this.baseUrl}/${assessmentId}/approve`, request);
  }

  declineRiskAssessment(assessmentId: string, request: DeclineRiskRequest): Observable<RiskAssessment> {
    return this.http.post<RiskAssessment>(`${this.baseUrl}/${assessmentId}/decline`, request);
  }
}
