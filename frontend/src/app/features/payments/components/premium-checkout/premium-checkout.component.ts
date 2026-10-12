import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectPaymentLoading, selectPaymentError, selectCurrentPremiumPayment } from '../../state/payment.selectors';
import * as PaymentActions from '../../state/payment.actions';

import { PolicyService } from '../../../quotes-policies/services/policy.service';
import { Quote } from '../../../quotes-policies/state/policy.models';
import { PaymentService } from '../../services/payment.service';
import { PremiumPayment } from '../../state/payment.models';

@Component({
  selector: 'app-premium-checkout',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './premium-checkout.component.html',
  styleUrls: ['./premium-checkout.component.css']
})
export class PremiumCheckoutComponent implements OnInit {
  quoteId!: string;
  quote$: Observable<Quote> | null = null;
  checkoutForm: FormGroup;
  loading$: Observable<boolean>;
  error$: Observable<string | null>;
  
  isPaymentDoneForMonth: boolean = false;
  existingPayment: PremiumPayment | null = null;
  currentMonthName: string = '';

  constructor(
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private store: Store,
    private policyService: PolicyService,
    private paymentService: PaymentService
  ) {
    this.checkoutForm = this.fb.group({
      paymentProvider: ['RAZORPAY_GATEWAY', Validators.required]
    });
    this.loading$ = this.store.select(selectPaymentLoading);
    this.error$ = this.store.select(selectPaymentError);

    const date = new Date();
    this.currentMonthName = date.toLocaleString('default', { month: 'long', year: 'numeric' });
  }

  ngOnInit(): void {
    this.quoteId = this.route.snapshot.paramMap.get('id') || '';
    if (this.quoteId) {
      this.quote$ = this.policyService.getQuoteById(this.quoteId);
      this.checkMonthlyPaymentStatus();
    }

    // Also listen to current store payment state
    this.store.select(selectCurrentPremiumPayment).subscribe((current) => {
      if (current && (current.quoteId === this.quoteId || !this.quoteId) && current.status === 'SUCCESS') {
        this.isPaymentDoneForMonth = true;
        this.existingPayment = current;
      }
    });
  }

  checkMonthlyPaymentStatus(): void {
    this.paymentService.getAllPremiumPayments().subscribe({
      next: (payments) => {
        if (payments && payments.length > 0) {
          const matchingPayment = payments.find((p) => {
            const isMatch = p.quoteId === this.quoteId;
            const isSuccess = p.status === 'SUCCESS';
            return isMatch && isSuccess;
          });

          if (matchingPayment) {
            this.isPaymentDoneForMonth = true;
            this.existingPayment = matchingPayment;
          }
        }
      },
      error: () => {
        // Fallback: keep normal flow if payment ledger is not yet populated
      }
    });
  }

  onSubmitPayment(): void {
    if (this.isPaymentDoneForMonth) {
      return;
    }
    if (this.checkoutForm.valid && this.quoteId) {
      this.store.dispatch(
        PaymentActions.createPremiumPayment({
          request: {
            quoteId: this.quoteId,
            paymentProvider: this.checkoutForm.value.paymentProvider
          }
        })
      );
    }
  }
}
