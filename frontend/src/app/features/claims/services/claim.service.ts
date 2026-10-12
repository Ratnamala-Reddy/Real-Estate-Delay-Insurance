import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Claim, CreateClaimRequest } from '../state/claim.models';

@Injectable({
  providedIn: 'root'
})
export class ClaimService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/claims`;

  constructor(private http: HttpClient) {}

  getClaims(): Observable<Claim[]> {
    return this.http.get<Claim[]>(this.baseUrl);
  }

  getClaimById(claimId: string): Observable<Claim> {
    if (!claimId?.trim()) {
      return throwError(() => new Error('Claim ID is required.'));
    }
    return this.http.get<Claim>(`${this.baseUrl}/${claimId.trim()}`);
  }

  createClaim(request: CreateClaimRequest): Observable<Claim> {
    return this.http.post<Claim>(this.baseUrl, request);
  }

  calculateDelay(claimId: string): Observable<Claim> {
    if (!claimId?.trim()) {
      return throwError(() => new Error('Claim ID is required for delay calculation.'));
    }
    return this.http.post<Claim>(`${this.baseUrl}/${claimId.trim()}/delay-calculation`, {});
  }

  calculateCompensation(claimId: string): Observable<Claim> {
    if (!claimId?.trim()) {
      return throwError(() => new Error('Claim ID is required for compensation calculation.'));
    }
    return this.http.post<Claim>(`${this.baseUrl}/${claimId.trim()}/compensation-calculation`, {});
  }

  verifyInsuranceEligibility(claimId: string): Observable<Claim> {
    if (!claimId?.trim()) {
      return throwError(() => new Error('Claim ID is required for eligibility verification.'));
    }
    return this.http.post<Claim>(`${this.baseUrl}/${claimId.trim()}/insurance-verification`, {});
  }

  approveClaim(claimId: string): Observable<Claim> {
    if (!claimId?.trim()) {
      return throwError(() => new Error('Claim ID is required for claim approval.'));
    }
    return this.http.post<Claim>(`${this.baseUrl}/${claimId.trim()}/approve`, {});
  }

  rejectClaim(claimId: string): Observable<Claim> {
    if (!claimId?.trim()) {
      return throwError(() => new Error('Claim ID is required for claim rejection.'));
    }
    return this.http.post<Claim>(`${this.baseUrl}/${claimId.trim()}/reject`, {});
  }

  closeClaim(claimId: string): Observable<Claim> {
    if (!claimId?.trim()) {
      return throwError(() => new Error('Claim ID is required for closing claim.'));
    }
    return this.http.post<Claim>(`${this.baseUrl}/${claimId.trim()}/close`, {});
  }
}
