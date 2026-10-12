import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectAllQuotes, selectPolicyLoading } from '../../state/policy.selectors';
import * as PolicyActions from '../../state/policy.actions';
import { Quote } from '../../state/policy.models';
import { selectUserRole } from '../../../auth/state/auth.selectors';
import { PaymentService } from '../../../payments/services/payment.service';
import { PremiumPayment } from '../../../payments/state/payment.models';

import { ApplicationService } from '../../../applications/services/application.service';
import { RiskService } from '../../../risk/services/risk.service';
import { RiskAssessment } from '../../../risk/state/risk.models';

@Component({
  selector: 'app-quote-selection',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './quote-selection.component.html',
  styleUrls: ['./quote-selection.component.css']
})
export class QuoteSelectionComponent implements OnInit {
  applicationId!: string;
  quotes$: Observable<Quote[]>;
  loading$: Observable<boolean>;
  userRole$: Observable<string | null>;
  generateForm: FormGroup;
  paidQuoteIds = new Set<string>();
  riskAssessment: RiskAssessment | null = null;
  propertyValue: number = 5000000;

  constructor(
    private route: ActivatedRoute,
    private store: Store,
    private fb: FormBuilder,
    private paymentService: PaymentService,
    private applicationService: ApplicationService,
    private riskService: RiskService
  ) {
    this.quotes$ = this.store.select(selectAllQuotes);
    this.loading$ = this.store.select(selectPolicyLoading);
    this.userRole$ = this.store.select(selectUserRole);

    this.generateForm = this.fb.group({
      baseCoverageAmount: [5000000, [Validators.required, Validators.min(100000)]]
    });
  }

  selectedQuoteForConfirmation: Quote | null = null;
  declineQuoteForConfirmation: Quote | null = null;

  ngOnInit(): void {
    this.applicationId = this.route.snapshot.paramMap.get('id') || '';
    if (this.applicationId) {
      this.store.dispatch(PolicyActions.loadQuotes({ applicationId: this.applicationId }));
      this.loadApplicationAndRiskData();
    }
    this.loadPaymentStatus();
  }

  loadApplicationAndRiskData(): void {
    this.applicationService.getApplicationById(this.applicationId).subscribe({
      next: (app) => {
        if (app && app.propertyValue) {
          this.propertyValue = app.propertyValue;
          this.generateForm.patchValue({ baseCoverageAmount: app.propertyValue });
        }
      },
      error: () => {}
    });

    this.riskService.getRiskAssessment(this.applicationId).subscribe({
      next: (risk) => {
        this.riskAssessment = risk;
      },
      error: () => {}
    });
  }

  loadPaymentStatus(): void {
    this.paymentService.getAllPremiumPayments().subscribe({
      next: (payments: PremiumPayment[]) => {
        if (payments && payments.length > 0) {
          payments.forEach((p) => {
            if (p.status === 'SUCCESS' && p.quoteId) {
              this.paidQuoteIds.add(p.quoteId);
            }
          });
        }
      },
      error: () => {}
    });
  }

  isQuotePaid(quoteId: string): boolean {
    return this.paidQuoteIds.has(quoteId);
  }

  onGenerateQuotes(): void {
    if (this.generateForm.valid) {
      const amount = Number(this.generateForm.value.baseCoverageAmount);
      this.store.dispatch(
        PolicyActions.generateQuotes({
          applicationId: this.applicationId,
          request: {
            applicationId: this.applicationId,
            baseCoverageAmount: amount,
            coverageAmount: amount,
            generatedBy: 'Underwriter Admin'
          }
        })
      );
    }
  }

  openSelectConfirmation(quote: Quote): void {
    this.selectedQuoteForConfirmation = quote;
  }

  cancelSelectConfirmation(): void {
    this.selectedQuoteForConfirmation = null;
  }

  confirmAccept(): void {
    if (this.selectedQuoteForConfirmation) {
      this.store.dispatch(PolicyActions.acceptQuote({ quoteId: this.selectedQuoteForConfirmation.quoteId }));
      this.selectedQuoteForConfirmation = null;
    }
  }

  openDeclineConfirmation(quote: Quote): void {
    this.declineQuoteForConfirmation = quote;
  }

  cancelDeclineConfirmation(): void {
    this.declineQuoteForConfirmation = null;
  }

  confirmDecline(): void {
    if (this.declineQuoteForConfirmation) {
      this.store.dispatch(PolicyActions.declineQuote({ quoteId: this.declineQuoteForConfirmation.quoteId }));
      this.declineQuoteForConfirmation = null;
    }
  }
}
