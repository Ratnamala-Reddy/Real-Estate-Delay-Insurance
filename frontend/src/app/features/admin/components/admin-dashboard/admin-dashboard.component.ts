import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Observable, map, of, catchError } from 'rxjs';
import { selectAllAdminUsers } from '../../state/admin.selectors';
import * as AdminActions from '../../state/admin.actions';
import * as ApplicationActions from '../../../applications/state/application.actions';
import * as PolicyActions from '../../../quotes-policies/state/policy.actions';
import * as ClaimActions from '../../../claims/state/claim.actions';
import { selectAllApplications } from '../../../applications/state/application.selectors';
import { selectAllPolicies } from '../../../quotes-policies/state/policy.selectors';
import { selectAllClaims } from '../../../claims/state/claim.selectors';
import { Application } from '../../../applications/state/application.models';
import { Policy } from '../../../quotes-policies/state/policy.models';
import { Claim } from '../../../claims/state/claim.models';
import { PaymentService } from '../../../payments/services/payment.service';
import { NotificationService } from '../../../../core/services/notification.service';

import { selectCurrentUser } from '../../../auth/state/auth.selectors';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent implements OnInit {
  currentUser$: Observable<any>;
  users$: Observable<any[]>;
  applications$: Observable<Application[]>;
  policies$: Observable<Policy[]>;
  claims$: Observable<Claim[]>;
  allPayments$: Observable<any[]> | null = null;
  allCompensationPayments$: Observable<any[]> | null = null;
  loadingPayments = false;
  loadingCompensationPayments = false;

  pendingVerificationsCount$: Observable<number>;
  verifiedCount$: Observable<number>;
  totalAppsCount$: Observable<number>;
  activePoliciesCount$: Observable<number>;
  activeClaimsCount$: Observable<number>;

  activeTab: 'claims' | 'escrow' | 'payouts' = 'claims';
  manualPaymentId: string = '';
  settlingWebhook: boolean = false;
  settlingPaymentId: string | null = null;
  settlingCompensationId: string | null = null;
  webhookSuccessMsg: string | null = null;
  webhookErrorMsg: string | null = null;

  setActiveTab(tab: 'claims' | 'escrow' | 'payouts'): void {
    this.activeTab = tab;
  }

  constructor(
    private store: Store,
    private paymentService: PaymentService,
    private notificationService: NotificationService
  ) {
    this.currentUser$ = this.store.select(selectCurrentUser);
    this.users$ = this.store.select(selectAllAdminUsers);
    this.applications$ = this.store.select(selectAllApplications);
    this.policies$ = this.store.select(selectAllPolicies);
    this.claims$ = this.store.select(selectAllClaims);

    this.totalAppsCount$ = this.applications$.pipe(map(apps => apps.length));
    this.pendingVerificationsCount$ = this.applications$.pipe(
      map(apps => apps.filter(a => a.status === 'SUBMITTED' || a.status === 'VERIFICATION_IN_PROGRESS' || a.status === 'ADDITIONAL_INFORMATION_REQUIRED').length)
    );
    this.verifiedCount$ = this.applications$.pipe(
      map(apps => apps.filter(a => a.status === 'VERIFIED').length)
    );
    this.activePoliciesCount$ = this.policies$.pipe(map(p => p.length));
    this.activeClaimsCount$ = this.claims$.pipe(
      map(claims => claims.filter(c => c.status !== 'CLOSED' && c.status !== 'REJECTED').length)
    );
  }

  ngOnInit(): void {
    this.store.dispatch(AdminActions.loadAllUsers());
    this.store.dispatch(ApplicationActions.loadAllApplications());
    this.store.dispatch(PolicyActions.loadPolicies());
    this.store.dispatch(ClaimActions.loadClaims());
    this.loadAllPayments();
    this.loadAllCompensationPayments();
  }

  loadAllPayments(): void {
    this.loadingPayments = true;
    this.allPayments$ = this.paymentService.getAllPremiumPayments().pipe(
      map(list => {
        setTimeout(() => { this.loadingPayments = false; }, 0);
        return list;
      }),
      catchError(err => {
        setTimeout(() => { this.loadingPayments = false; }, 0);
        console.warn('Could not fetch all payments:', err);
        return of([]);
      })
    );
  }

  loadAllCompensationPayments(): void {
    this.loadingCompensationPayments = true;
    this.allCompensationPayments$ = this.paymentService.getAllCompensationPayments().pipe(
      map(list => {
        setTimeout(() => { this.loadingCompensationPayments = false; }, 0);
        return list;
      }),
      catchError(err => {
        setTimeout(() => { this.loadingCompensationPayments = false; }, 0);
        console.warn('Could not fetch all compensation payments:', err);
        return of([]);
      })
    );
  }

  onSettlePayment(payment: any): void {
    if (!payment || !payment.paymentId) return;

    this.settlingPaymentId = payment.paymentId;
    this.webhookSuccessMsg = null;
    this.webhookErrorMsg = null;

    const request = {
      paymentId: payment.paymentId,
      transactionReference: payment.transactionReference || `TXN-ADM-${Date.now().toString().slice(-6)}`,
      paymentStatus: 'SUCCESS'
    };

    this.paymentService.processPremiumWebhook(request).subscribe({
      next: (res) => {
        this.settlingPaymentId = null;
        this.webhookSuccessMsg = `Payment ${res.paymentId} settled successfully! Active Policy has been auto-created.`;
        this.notificationService.success('Settled & Policy Created', this.webhookSuccessMsg);
        this.loadAllPayments();
        this.store.dispatch(PolicyActions.loadPolicies());
      },
      error: (err) => {
        this.settlingPaymentId = null;
        const msg = err?.error?.message || (err.status === 409 ? 'Payment is already finalized or policy already created.' : 'Failed to settle payment.');
        this.webhookErrorMsg = msg;
        this.notificationService.warning('Settlement Status', msg);
        this.loadAllPayments();
        this.store.dispatch(PolicyActions.loadPolicies());
      }
    });
  }

  onSettleCompensationPayment(payment: any): void {
    if (!payment || !payment.paymentId) return;

    this.settlingCompensationId = payment.paymentId;

    const request = {
      paymentId: payment.paymentId,
      transactionReference: payment.transactionReference || `TXN-COMP-${Date.now().toString().slice(-6)}`,
      paymentStatus: 'PAID'
    };

    this.paymentService.processCompensationWebhook(request).subscribe({
      next: (res) => {
        this.settlingCompensationId = null;
        this.notificationService.success('Compensation Disbursed', `Compensation payment of ₹${payment.payoutAmount?.toLocaleString()} disbursed. Claim finalized as CLOSED.`);
        this.loadAllCompensationPayments();
      },
      error: (err) => {
        this.settlingCompensationId = null;
        const msg = err?.error?.message || 'Failed to disburse compensation payout.';
        this.notificationService.error('Disbursement Failed', msg);
        this.loadAllCompensationPayments();
      }
    });
  }

  onTriggerPremiumWebhook(): void {
    if (!this.manualPaymentId.trim()) {
      this.notificationService.warning('Required', 'Please enter a Payment ID');
      return;
    }

    this.settlingWebhook = true;
    this.webhookSuccessMsg = null;
    this.webhookErrorMsg = null;

    const request = {
      paymentId: this.manualPaymentId.trim(),
      transactionReference: `TXN-ADM-${Date.now().toString().slice(-6)}`,
      paymentStatus: 'SUCCESS'
    };

    this.paymentService.processPremiumWebhook(request).subscribe({
      next: (res) => {
        this.settlingWebhook = false;
        this.webhookSuccessMsg = `Payment ${res.paymentId} marked as SUCCESS! Active Policy has been auto-created.`;
        this.notificationService.success('Webhook Processed', this.webhookSuccessMsg);
        this.loadAllPayments();
        this.store.dispatch(PolicyActions.loadPolicies());
      },
      error: (err) => {
        this.settlingWebhook = false;
        const msg = err?.error?.message || (err.status === 409 ? 'Payment is already finalized or policy already created.' : 'Failed to process payment webhook.');
        this.webhookErrorMsg = msg;
        this.notificationService.warning('Webhook Status', msg);
        this.loadAllPayments();
        this.store.dispatch(PolicyActions.loadPolicies());
      }
    });
  }
}
