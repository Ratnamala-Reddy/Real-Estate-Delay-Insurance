import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable, Subscription, map } from 'rxjs';
import { selectSelectedClaim, selectClaimLoading, selectAllClaims } from '../../state/claim.selectors';
import { selectUserRole } from '../../../auth/state/auth.selectors';
import * as ClaimActions from '../../state/claim.actions';
import { Claim } from '../../state/claim.models';
import { PaymentService } from '../../../payments/services/payment.service';
import { NotificationService } from '../../../../core/services/notification.service';

@Component({
  selector: 'app-claim-adjudication',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './claim-adjudication.component.html',
  styleUrls: ['./claim-adjudication.component.css']
})
export class ClaimAdjudicationComponent implements OnInit, OnDestroy {
  claimId: string = '';
  claim$: Observable<Claim | null>;
  allClaims$: Observable<Claim[]>;
  loading$: Observable<boolean>;
  userRole$: Observable<string | null>;
  isAdmin$: Observable<boolean>;
  isBuyer$: Observable<boolean>;
  disbursingPayout: boolean = false;
  private routeSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private store: Store,
    private paymentService: PaymentService,
    private notificationService: NotificationService
  ) {
    this.allClaims$ = this.store.select(selectAllClaims);
    this.loading$ = this.store.select(selectClaimLoading);
    this.userRole$ = this.store.select(selectUserRole);
    this.isAdmin$ = this.userRole$.pipe(map((role) => role === 'ADMIN'));
    this.isBuyer$ = this.userRole$.pipe(map((role) => role === 'BUYER'));

    this.claim$ = this.store.select(selectSelectedClaim).pipe(
      map((claim) => {
        if (!this.claimId) return null;
        if (claim && claim.claimId === this.claimId) return claim;
        return null;
      })
    );
  }

  ngOnInit(): void {
    this.store.dispatch(ClaimActions.loadClaims());
    this.routeSub = this.route.queryParams.subscribe((params) => {
      this.claimId = (params['claimId'] || '').trim();
      if (this.claimId) {
        this.store.dispatch(ClaimActions.loadClaimById({ claimId: this.claimId }));
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  private resolveClaimId(targetClaimId?: string): string | null {
    const id = (targetClaimId || this.claimId || '').trim();
    if (!id) {
      this.notificationService.warning('Claim Not Selected', 'Please select a claim from the queue before proceeding.');
      return null;
    }
    this.claimId = id;
    return id;
  }

  onCalculateDelay(claimId?: string): void {
    const id = this.resolveClaimId(claimId);
    if (id) {
      this.store.dispatch(ClaimActions.calculateDelay({ claimId: id }));
    }
  }

  onCalculateCompensation(claimId?: string): void {
    const id = this.resolveClaimId(claimId);
    if (id) {
      this.store.dispatch(ClaimActions.calculateCompensation({ claimId: id }));
    }
  }

  onVerifyEligibility(claimId?: string): void {
    const id = this.resolveClaimId(claimId);
    if (id) {
      this.store.dispatch(ClaimActions.verifyInsuranceEligibility({ claimId: id }));
    }
  }

  onApprove(claimId?: string): void {
    const id = this.resolveClaimId(claimId);
    if (id) {
      this.store.dispatch(ClaimActions.approveClaim({ claimId: id }));
    }
  }

  onReject(claimId?: string): void {
    const id = this.resolveClaimId(claimId);
    if (id) {
      this.store.dispatch(ClaimActions.rejectClaim({ claimId: id }));
    }
  }

  onDisbursePayout(claim: Claim): void {
    if (!claim || !claim.claimId) return;
    this.disbursingPayout = true;
    const currentClaimId = claim.claimId;

    this.paymentService.getAllCompensationPayments().subscribe({
      next: (payments) => {
        const matching = payments.find((p) => p.claimId === currentClaimId);
        if (matching && matching.paymentId) {
          const req = {
            paymentId: matching.paymentId,
            transactionReference: matching.transactionReference || `TXN-DISB-${Date.now().toString().slice(-6)}`,
            paymentStatus: 'PAID'
          };
          this.paymentService.processCompensationWebhook(req).subscribe({
            next: (res) => {
              this.disbursingPayout = false;
              this.notificationService.success(
                'Payout Disbursed',
                `Compensation ₹${matching.payoutAmount?.toLocaleString()} disbursed successfully. Claim closed.`
              );
              this.store.dispatch(ClaimActions.loadClaimById({ claimId: currentClaimId }));
            },
            error: (err) => {
              this.disbursingPayout = false;
              this.notificationService.error(
                'Payout Failed',
                err.error?.message || 'Failed to process payout.'
              );
            }
          });
        } else {
          this.disbursingPayout = false;
          this.notificationService.warning('Payment Record Missing', 'No compensation payment record found for this claim.');
        }
      },
      error: (err) => {
        this.disbursingPayout = false;
        this.notificationService.error('Error', 'Unable to fetch compensation payments.');
      }
    });
  }
}
