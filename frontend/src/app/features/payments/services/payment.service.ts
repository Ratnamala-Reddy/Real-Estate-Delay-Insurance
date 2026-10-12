import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  PremiumPayment,
  PremiumPaymentRequest,
  CompensationPayment,
  CompensationPaymentRequest
} from '../state/payment.models';

@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  createPremiumPayment(request: PremiumPaymentRequest): Observable<PremiumPayment> {
    return this.http.post<PremiumPayment>(`${this.baseUrl}/api/v1/premium-payments`, request);
  }

  getAllPremiumPayments(): Observable<PremiumPayment[]> {
    return this.http.get<PremiumPayment[]>(`${this.baseUrl}/api/v1/premium-payments`);
  }

  getPremiumPayment(paymentId: string): Observable<PremiumPayment> {
    return this.http.get<PremiumPayment>(`${this.baseUrl}/api/v1/premium-payments/${paymentId}`);
  }

  createCompensationPayment(request: CompensationPaymentRequest): Observable<CompensationPayment> {
    return this.http.post<CompensationPayment>(`${this.baseUrl}/api/v1/compensation-payments`, request);
  }

  getAllCompensationPayments(): Observable<CompensationPayment[]> {
    return this.http.get<CompensationPayment[]>(`${this.baseUrl}/api/v1/compensation-payments`);
  }

  getCompensationPayment(paymentId: string): Observable<CompensationPayment> {
    return this.http.get<CompensationPayment>(`${this.baseUrl}/api/v1/compensation-payments/${paymentId}`);
  }

  processPremiumWebhook(request: { paymentId: string; transactionReference: string; paymentStatus: string }): Observable<PremiumPayment> {
    return this.http.post<PremiumPayment>(`${this.baseUrl}/api/v1/payment-webhooks/premium`, request);
  }

  processCompensationWebhook(request: { paymentId: string; transactionReference: string; paymentStatus: string }): Observable<CompensationPayment> {
    return this.http.post<CompensationPayment>(`${this.baseUrl}/api/v1/payment-webhooks/compensation`, request);
  }
}
