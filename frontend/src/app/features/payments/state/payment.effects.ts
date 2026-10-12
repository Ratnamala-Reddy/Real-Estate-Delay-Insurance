import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { PaymentService } from '../services/payment.service';
import * as PaymentActions from './payment.actions';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { Router } from '@angular/router';

import { PolicyService } from '../../quotes-policies/services/policy.service';
import * as PolicyActions from '../../quotes-policies/state/policy.actions';

@Injectable()
export class PaymentEffects {
  constructor(
    private actions$: Actions,
    private paymentService: PaymentService,
    private policyService: PolicyService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  createPremiumPayment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PaymentActions.createPremiumPayment),
      switchMap(({ request }) =>
        this.paymentService.createPremiumPayment(request).pipe(
          map((payment) => PaymentActions.createPremiumPaymentSuccess({ payment })),
          catchError((error) =>
            of(PaymentActions.createPremiumPaymentFailure({ error: error.error?.message || 'Premium payment failed.' }))
          )
        )
      )
    )
  );

  createPremiumPaymentSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(PaymentActions.createPremiumPaymentSuccess),
        tap(({ payment }) => {
          this.notificationService.success(
            'Payment Confirmed!',
            `Premium payment of ₹${payment.premiumAmount} recorded in escrow (Ref: ${payment.paymentId ? payment.paymentId.substring(0, 8) : 'TXN'}).`
          );
          this.router.navigate(['/payments/history']);
        })
      ),
    { dispatch: false }
  );

  createPremiumPaymentFailure$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(PaymentActions.createPremiumPaymentFailure),
        tap(({ error }) => {
          this.notificationService.error('Payment Failed', error);
        })
      ),
    { dispatch: false }
  );

  loadPremiumPayment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PaymentActions.loadPremiumPayment),
      switchMap(({ paymentId }) =>
        this.paymentService.getPremiumPayment(paymentId).pipe(
          map((payment) => PaymentActions.loadPremiumPaymentSuccess({ payment })),
          catchError((error) =>
            of(PaymentActions.loadPremiumPaymentFailure({ error: error.error?.message || 'Failed to load payment.' }))
          )
        )
      )
    )
  );

  createCompensationPayment$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PaymentActions.createCompensationPayment),
      switchMap(({ request }) =>
        this.paymentService.createCompensationPayment(request).pipe(
          map((payment) => PaymentActions.createCompensationPaymentSuccess({ payment })),
          catchError((error) =>
            of(PaymentActions.createCompensationPaymentFailure({ error: error.error?.message || 'Compensation payout failed.' }))
          )
        )
      )
    )
  );
}
