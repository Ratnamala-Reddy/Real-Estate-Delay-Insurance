import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Quote,
  Policy,
  GenerateQuoteRequest,
  CreatePolicyRequest
} from '../state/policy.models';

@Injectable({
  providedIn: 'root'
})
export class PolicyService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  generateQuotes(applicationId: string, request: GenerateQuoteRequest): Observable<Quote[]> {
    return this.http.post<Quote[]>(`${this.baseUrl}/api/v1/quotes/applications/${applicationId}`, request);
  }

  getQuotesByApplication(applicationId: string): Observable<Quote[]> {
    return this.http.get<Quote[]>(`${this.baseUrl}/api/v1/quotes/applications/${applicationId}`);
  }

  getQuoteById(quoteId: string): Observable<Quote> {
    return this.http.get<Quote>(`${this.baseUrl}/api/v1/quotes/${quoteId}`);
  }

  acceptQuote(quoteId: string): Observable<Quote> {
    return this.http.post<Quote>(`${this.baseUrl}/api/v1/quotes/${quoteId}/accept`, {});
  }

  declineQuote(quoteId: string): Observable<Quote> {
    return this.http.post<Quote>(`${this.baseUrl}/api/v1/quotes/${quoteId}/decline`, {});
  }

  getPolicies(): Observable<Policy[]> {
    return this.http.get<Policy[]>(`${this.baseUrl}/api/v1/policies`);
  }

  getPolicyById(policyId: string): Observable<Policy> {
    return this.http.get<Policy>(`${this.baseUrl}/api/v1/policies/${policyId}`);
  }

  createPolicy(request: CreatePolicyRequest): Observable<Policy> {
    return this.http.post<Policy>(`${this.baseUrl}/api/v1/policies`, request);
  }

  completePolicy(policyId: string): Observable<Policy> {
    return this.http.post<Policy>(`${this.baseUrl}/api/v1/policies/${policyId}/complete`, {});
  }

  closePolicy(policyId: string): Observable<Policy> {
    return this.http.post<Policy>(`${this.baseUrl}/api/v1/policies/${policyId}/close`, {});
  }
}
