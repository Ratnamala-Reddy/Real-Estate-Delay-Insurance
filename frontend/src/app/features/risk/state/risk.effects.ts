import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { RiskService } from '../services/risk.service';
import * as RiskActions from './risk.actions';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';
import { Router } from '@angular/router';

@Injectable()
export class RiskEffects {
  constructor(
    private actions$: Actions,
    private riskService: RiskService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  calculateRisk$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.calculateRisk),
      switchMap(({ request }) =>
        this.riskService.calculateRisk(request).pipe(
          map((assessment) => RiskActions.calculateRiskSuccess({ assessment })),
          catchError((error) =>
            of(RiskActions.calculateRiskFailure({ error: error.error?.message || 'Risk calculation failed.' }))
          )
        )
      )
    )
  );

  loadRisk$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.loadRiskAssessment),
      switchMap(({ applicationId }) =>
        this.riskService.getRiskAssessment(applicationId).pipe(
          map((assessment) => RiskActions.loadRiskAssessmentSuccess({ assessment })),
          catchError((error) =>
            of(RiskActions.loadRiskAssessmentFailure({ error: error.error?.message || 'Failed to load risk assessment.' }))
          )
        )
      )
    )
  );

  approveRisk$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.approveRiskAssessment),
      switchMap(({ assessmentId, request }) =>
        this.riskService.approveRiskAssessment(assessmentId, request).pipe(
          map((assessment) => RiskActions.approveRiskAssessmentSuccess({ assessment })),
          catchError((error) =>
            of(RiskActions.approveRiskAssessmentFailure({ error: error.error?.message || 'Risk approval failed.' }))
          )
        )
      )
    )
  );

  approveSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(RiskActions.approveRiskAssessmentSuccess),
        tap(({ assessment }) => {
          this.notificationService.success('Risk Approved', `Risk score approved. Multiplier: ${assessment.premiumMultiplier}`);
          this.router.navigate(['/quotes/applications', assessment.applicationId]);
        })
      ),
    { dispatch: false }
  );

  calculateRiskSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(RiskActions.calculateRiskSuccess),
        tap(({ assessment }) => {
          this.notificationService.success(
            'Risk Calculated',
            `Assessed Category: ${assessment.riskCategory} (Score: ${assessment.riskScore}, Multiplier: ${assessment.premiumMultiplier}x)`
          );
        })
      ),
    { dispatch: false }
  );

  declineRisk$ = createEffect(() =>
    this.actions$.pipe(
      ofType(RiskActions.declineRiskAssessment),
      switchMap(({ assessmentId, request }) =>
        this.riskService.declineRiskAssessment(assessmentId, request).pipe(
          map((assessment) => RiskActions.declineRiskAssessmentSuccess({ assessment })),
          catchError((error) =>
            of(RiskActions.declineRiskAssessmentFailure({ error: error.error?.message || 'Risk decline failed.' }))
          )
        )
      )
    )
  );

  declineSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(RiskActions.declineRiskAssessmentSuccess),
        tap(({ assessment }) => {
          this.notificationService.warning('Risk Declined', `Risk assessment marked as DECLINED.`);
        })
      ),
    { dispatch: false }
  );
}
