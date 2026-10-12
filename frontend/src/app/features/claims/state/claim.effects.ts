import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { ClaimService } from '../services/claim.service';
import * as ClaimActions from './claim.actions';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { Router } from '@angular/router';

@Injectable()
export class ClaimEffects {
  constructor(
    private actions$: Actions,
    private claimService: ClaimService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  loadClaims$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ClaimActions.loadClaims),
      switchMap(() =>
        this.claimService.getClaims().pipe(
          map((claims) => ClaimActions.loadClaimsSuccess({ claims })),
          catchError((error) =>
            of(ClaimActions.loadClaimsFailure({ error: error.error?.message || 'Failed to load claims.' }))
          )
        )
      )
    )
  );

  loadClaimById$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ClaimActions.loadClaimById),
      switchMap(({ claimId }) =>
        this.claimService.getClaimById(claimId).pipe(
          map((claim) => ClaimActions.loadClaimByIdSuccess({ claim })),
          catchError((error) =>
            of(ClaimActions.loadClaimByIdFailure({ error: error.error?.message || 'Failed to load claim.' }))
          )
        )
      )
    )
  );

  createClaim$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ClaimActions.createClaim),
      switchMap(({ request }) =>
        this.claimService.createClaim(request).pipe(
          map((claim) => ClaimActions.createClaimSuccess({ claim })),
          catchError((error) => {
            let errorMsg = error.error?.message;
            if (errorMsg && (errorMsg.includes('uk_claim_policy') || errorMsg.includes('Duplicate entry') || errorMsg.includes('SQL [') || errorMsg.includes('executeMany') || errorMsg.includes('DataIntegrityViolation'))) {
              errorMsg = 'A claim has already been registered for this policy ID. Each insurance policy can have only one claim submitted.';
            } else if (!errorMsg) {
              if (error.status === 409) {
                errorMsg = 'A claim has already been submitted for this policy ID, or policy is not currently eligible.';
              } else if (error.status === 403) {
                errorMsg = 'Access denied: You can only file claims on your own policies.';
              } else if (error.status === 404) {
                errorMsg = 'Policy not found. Please ensure the policy is active.';
              } else {
                errorMsg = 'Failed to file claim. Please check the policy details and eligibility.';
              }
            }
            this.notificationService.warning('Claim Submission Notice', errorMsg);
            return of(ClaimActions.createClaimFailure({ error: errorMsg }));
          })
        )
      )
    )
  );

  createClaimSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(ClaimActions.createClaimSuccess),
        tap(({ claim }) => {
          this.notificationService.success('Claim Filed Successfully', `Claim #${claim.claimId} submitted for delay adjudication.`);
          this.router.navigate(['/claims']);
        })
      ),
    { dispatch: false }
  );

  calculateDelay$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ClaimActions.calculateDelay),
      switchMap(({ claimId }) =>
        this.claimService.calculateDelay(claimId).pipe(
          map((claim) => {
            this.notificationService.success(
              'Delay Verified',
              `Delay calculation verified: ${claim.eligibleDelayMonths || 0} eligible month(s).`
            );
            return ClaimActions.updateClaimSuccess({ claim });
          }),
          catchError((error) => {
            const msg = error.error?.message || 'Delay calculation failed.';
            this.notificationService.error('Delay Calculation Error', msg);
            return of(ClaimActions.updateClaimFailure({ error: msg }));
          })
        )
      )
    )
  );

  calculateCompensation$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ClaimActions.calculateCompensation),
      switchMap(({ claimId }) =>
        this.claimService.calculateCompensation(claimId).pipe(
          map((claim) => {
            this.notificationService.success(
              'Compensation Calculated',
              `Calculated payout amount: ₹${claim.approvedAmount?.toLocaleString() || 0}.`
            );
            return ClaimActions.updateClaimSuccess({ claim });
          }),
          catchError((error) => {
            const msg = error.error?.message || 'Compensation calculation failed.';
            this.notificationService.error('Compensation Calculation Error', msg);
            return of(ClaimActions.updateClaimFailure({ error: msg }));
          })
        )
      )
    )
  );

  verifyEligibility$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ClaimActions.verifyInsuranceEligibility),
      switchMap(({ claimId }) =>
        this.claimService.verifyInsuranceEligibility(claimId).pipe(
          map((claim) => {
            this.notificationService.success(
              'Eligibility Verified',
              'Insurance policy eligibility verified for claim approval.'
            );
            return ClaimActions.updateClaimSuccess({ claim });
          }),
          catchError((error) => {
            const msg = error.error?.message || 'Policy eligibility verification failed.';
            this.notificationService.error('Eligibility Verification Error', msg);
            return of(ClaimActions.updateClaimFailure({ error: msg }));
          })
        )
      )
    )
  );

  approveClaim$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ClaimActions.approveClaim),
      switchMap(({ claimId }) =>
        this.claimService.approveClaim(claimId).pipe(
          map((claim) => {
            this.notificationService.success(
              'Claim Approved',
              'Claim approved and compensation payout transaction initiated in Payment Service.'
            );
            return ClaimActions.updateClaimSuccess({ claim });
          }),
          catchError((error) => {
            const msg = error.error?.message || 'Claim approval failed.';
            this.notificationService.error('Claim Approval Error', msg);
            return of(ClaimActions.updateClaimFailure({ error: msg }));
          })
        )
      )
    )
  );

  rejectClaim$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ClaimActions.rejectClaim),
      switchMap(({ claimId }) =>
        this.claimService.rejectClaim(claimId).pipe(
          map((claim) => {
            this.notificationService.warning(
              'Claim Rejected',
              'Claim has been rejected.'
            );
            return ClaimActions.updateClaimSuccess({ claim });
          }),
          catchError((error) => {
            const msg = error.error?.message || 'Claim rejection failed.';
            this.notificationService.error('Claim Rejection Error', msg);
            return of(ClaimActions.updateClaimFailure({ error: msg }));
          })
        )
      )
    )
  );
}
