import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { VerificationService } from '../services/verification.service';
import * as VerificationActions from './verification.actions';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { Router } from '@angular/router';

@Injectable()
export class VerificationEffects {
  constructor(
    private actions$: Actions,
    private verificationService: VerificationService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  loadQueue$ = createEffect(() =>
    this.actions$.pipe(
      ofType(VerificationActions.loadVerificationQueue),
      switchMap(() =>
        this.verificationService.getVerificationQueue().pipe(
          map((queue) => VerificationActions.loadVerificationQueueSuccess({ queue })),
          catchError((error) =>
            of(VerificationActions.loadVerificationQueueFailure({ error: error.error?.message || 'Failed to load queue.' }))
          )
        )
      )
    )
  );

  loadResult$ = createEffect(() =>
    this.actions$.pipe(
      ofType(VerificationActions.loadVerificationResult),
      switchMap(({ applicationId }) =>
        this.verificationService.getVerificationResult(applicationId).pipe(
          map((record) => VerificationActions.loadVerificationResultSuccess({ record })),
          catchError((error) =>
            of(VerificationActions.loadVerificationResultFailure({ error: error.error?.message || 'Failed to load verification.' }))
          )
        )
      )
    )
  );

  verifyProperty$ = createEffect(() =>
    this.actions$.pipe(
      ofType(VerificationActions.verifyProperty),
      switchMap(({ applicationId, request }) =>
        this.verificationService.verifyProperty(applicationId, request).pipe(
          map((record) => VerificationActions.verifyPropertySuccess({ record })),
          catchError((error) =>
            of(VerificationActions.verifyPropertyFailure({ error: error.error?.message || 'Verification update failed.' }))
          )
        )
      )
    )
  );

  approveVerification$ = createEffect(() =>
    this.actions$.pipe(
      ofType(VerificationActions.approveVerification),
      switchMap(({ applicationId, request }) =>
        this.verificationService.approveVerification(applicationId, request).pipe(
          map((record) => VerificationActions.approveVerificationSuccess({ record })),
          catchError((error) =>
            of(VerificationActions.approveVerificationFailure({ error: error.error?.message || 'Approval failed.' }))
          )
        )
      )
    )
  );

  approveSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(VerificationActions.approveVerificationSuccess),
        tap(({ record }) => {
          this.notificationService.success('Application Verified', 'Project and application approved for underwriting.');
          this.router.navigate(['/risk/calculator', record.applicationId]);
        })
      ),
    { dispatch: false }
  );

  requestInfo$ = createEffect(() =>
    this.actions$.pipe(
      ofType(VerificationActions.requestAdditionalInfo),
      switchMap(({ applicationId, request }) =>
        this.verificationService.requestInformation(applicationId, request).pipe(
          map((record) => VerificationActions.requestAdditionalInfoSuccess({ record })),
          catchError((error) =>
            of(VerificationActions.requestAdditionalInfoFailure({ error: error.error?.message || 'Failed to request info.' }))
          )
        )
      )
    )
  );

  rejectVerification$ = createEffect(() =>
    this.actions$.pipe(
      ofType(VerificationActions.rejectVerification),
      switchMap(({ applicationId, request }) =>
        this.verificationService.rejectApplication(applicationId, request).pipe(
          map((record) => VerificationActions.rejectVerificationSuccess({ record })),
          catchError((error) =>
            of(VerificationActions.rejectVerificationFailure({ error: error.error?.message || 'Rejection failed.' }))
          )
        )
      )
    )
  );
}
