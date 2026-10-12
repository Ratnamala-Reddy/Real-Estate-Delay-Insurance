import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  ApplicationQueueItem,
  VerificationRecord,
  PropertyVerificationRequest,
  AdditionalInfoRequest,
  ApprovalRequest,
  RejectVerificationRequest
} from '../state/verification.models';

@Injectable({
  providedIn: 'root'
})
export class VerificationService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/verifications`;

  constructor(private http: HttpClient) {}

  getVerificationQueue(): Observable<ApplicationQueueItem[]> {
    return this.http.get<ApplicationQueueItem[]>(`${this.baseUrl}/applications`);
  }

  getVerificationResult(applicationId: string): Observable<VerificationRecord> {
    return this.http.get<VerificationRecord>(`${this.baseUrl}/applications/${applicationId}`);
  }

  verifyProperty(applicationId: string, request: PropertyVerificationRequest): Observable<VerificationRecord> {
    return this.http.post<VerificationRecord>(`${this.baseUrl}/applications/${applicationId}/property-verification`, request);
  }

  requestInformation(applicationId: string, request: AdditionalInfoRequest): Observable<VerificationRecord> {
    return this.http.post<VerificationRecord>(`${this.baseUrl}/applications/${applicationId}/request-information`, request);
  }

  approveVerification(applicationId: string, request: ApprovalRequest): Observable<VerificationRecord> {
    return this.http.post<VerificationRecord>(`${this.baseUrl}/applications/${applicationId}/approve`, request);
  }

  rejectApplication(applicationId: string, request: RejectVerificationRequest): Observable<VerificationRecord> {
    return this.http.post<VerificationRecord>(`${this.baseUrl}/applications/${applicationId}/reject`, request);
  }
}
