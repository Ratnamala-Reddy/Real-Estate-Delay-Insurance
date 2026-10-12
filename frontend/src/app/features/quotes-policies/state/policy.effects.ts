import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { PolicyService } from '../services/policy.service';
import * as PolicyActions from './policy.actions';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { Router } from '@angular/router';

@Injectable()
export class PolicyEffects {
  constructor(
    private actions$: Actions,
    private policyService: PolicyService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  generateQuotes$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PolicyActions.generateQuotes),
      switchMap(({ applicationId, request }) =>
        this.policyService.generateQuotes(applicationId, request).pipe(
          map((quotes) => PolicyActions.generateQuotesSuccess({ quotes })),
          catchError((error) =>
            of(PolicyActions.generateQuotesFailure({ error: error.error?.message || 'Quote generation failed.' }))
          )
        )
      )
    )
  );

  loadQuotes$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PolicyActions.loadQuotes),
      switchMap(({ applicationId }) =>
        this.policyService.getQuotesByApplication(applicationId).pipe(
          map((quotes) => PolicyActions.loadQuotesSuccess({ quotes })),
          catchError((error) =>
            of(PolicyActions.loadQuotesFailure({ error: error.error?.message || 'Failed to load quotes.' }))
          )
        )
      )
    )
  );

  acceptQuote$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PolicyActions.acceptQuote),
      switchMap(({ quoteId }) =>
        this.policyService.acceptQuote(quoteId).pipe(
          map((quote) => PolicyActions.acceptQuoteSuccess({ quote })),
          catchError((error) =>
            of(PolicyActions.acceptQuoteFailure({ error: error.error?.message || 'Failed to accept quote.' }))
          )
        )
      )
    )
  );

  acceptQuoteSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(PolicyActions.acceptQuoteSuccess),
        tap(({ quote }) => {
          this.notificationService.success(
            'Plan Confirmed & Selected',
            `You have successfully selected the ${quote.planName} Plan (₹${quote.premiumAmount?.toLocaleString()}). Review your summary below to proceed to checkout.`
          );
        })
      ),
    { dispatch: false }
  );

  loadPolicies$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PolicyActions.loadPolicies),
      switchMap(() =>
        this.policyService.getPolicies().pipe(
          map((policies) => PolicyActions.loadPoliciesSuccess({ policies })),
          catchError((error) =>
            of(PolicyActions.loadPoliciesFailure({ error: error.error?.message || 'Failed to load policies.' }))
          )
        )
      )
    )
  );

  loadPolicyById$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PolicyActions.loadPolicyById),
      switchMap(({ policyId }) =>
        this.policyService.getPolicyById(policyId).pipe(
          map((policy) => PolicyActions.loadPolicyByIdSuccess({ policy })),
          catchError((error) =>
            of(PolicyActions.loadPolicyByIdFailure({ error: error.error?.message || 'Failed to load policy.' }))
          )
        )
      )
    )
  );
}
