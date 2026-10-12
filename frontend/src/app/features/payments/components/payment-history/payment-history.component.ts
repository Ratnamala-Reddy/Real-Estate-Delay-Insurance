import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable, of, catchError, map } from 'rxjs';
import { selectCurrentPremiumPayment, selectPaymentLoading } from '../../state/payment.selectors';
import { PremiumPayment } from '../../state/payment.models';
import { PaymentService } from '../../services/payment.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { selectUserRole } from '../../../auth/state/auth.selectors';

@Component({
  selector: 'app-payment-history',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './payment-history.component.html',
  styleUrls: ['./payment-history.component.css']
})
export class PaymentHistoryComponent implements OnInit {
  payment$: Observable<PremiumPayment | null>;
  allPayments$: Observable<PremiumPayment[]> | null = null;
  compensationPayments$: Observable<any[]> | null = null;
  loading$: Observable<boolean>;
  userRole$: Observable<string | null>;
  isBuyer$: Observable<boolean>;
  isAdmin$: Observable<boolean>;
  activeTab: 'premium' | 'compensation' = 'premium';
  settling = false;
  settledPayment: PremiumPayment | null = null;
  errorMessage: string | null = null;

  constructor(
    private store: Store,
    private paymentService: PaymentService,
    private notificationService: NotificationService,
    private router: Router
  ) {
    this.payment$ = this.store.select(selectCurrentPremiumPayment);
    this.loading$ = this.store.select(selectPaymentLoading);
    this.userRole$ = this.store.select(selectUserRole);
    this.isBuyer$ = this.userRole$.pipe(map(role => role === 'BUYER'));
    this.isAdmin$ = this.userRole$.pipe(map(role => role === 'ADMIN'));
  }

  ngOnInit(): void {
    this.loadAllPayments();
    this.loadCompensationPayments();
  }

  setTab(tab: 'premium' | 'compensation'): void {
    this.activeTab = tab;
  }

  loadAllPayments(): void {
    this.allPayments$ = this.paymentService.getAllPremiumPayments().pipe(
      catchError((err) => {
        console.warn('Could not load premium payments:', err);
        return of([]);
      })
    );
  }

  loadCompensationPayments(): void {
    this.compensationPayments$ = this.paymentService.getAllCompensationPayments().pipe(
      catchError((err) => {
        console.warn('Could not load compensation payments:', err);
        return of([]);
      })
    );
  }

  onSettlePayment(payment: PremiumPayment): void {
    if (!payment || !payment.paymentId) return;

    this.settling = true;
    this.errorMessage = null;

    const request = {
      paymentId: payment.paymentId,
      transactionReference: payment.transactionReference || `TXN-${payment.paymentId.substring(0, 8).toUpperCase()}`,
      paymentStatus: 'SUCCESS'
    };

    this.paymentService.processPremiumWebhook(request).subscribe({
      next: (updatedPayment) => {
        this.settling = false;
        this.settledPayment = updatedPayment;
        this.notificationService.success(
          'Policy Issued Successfully!',
          'Escrow payment confirmed and your Active Project Delay Insurance Policy has been created.'
        );
      },
      error: (err) => {
        this.settling = false;
        this.errorMessage = err?.error?.message || 'Failed to settle escrow payment.';
        this.notificationService.error('Settlement Failed', this.errorMessage || '');
      }
    });
  }
}
