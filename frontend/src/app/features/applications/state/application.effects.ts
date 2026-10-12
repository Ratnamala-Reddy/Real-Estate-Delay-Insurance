import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { ApplicationService } from '../services/application.service';
import * as ApplicationActions from './application.actions';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { NotificationService } from '../../../core/services/notification.service';

@Injectable()
export class ApplicationEffects {
  constructor(
    private actions$: Actions,
    private applicationService: ApplicationService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  loadMyApplications$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ApplicationActions.loadMyApplications),
      switchMap(() =>
        this.applicationService.getMyApplications().pipe(
          map((applications) => ApplicationActions.loadMyApplicationsSuccess({ applications })),
          catchError((error) =>
            of(ApplicationActions.loadMyApplicationsFailure({ error: error.error?.message || 'Failed to load applications.' }))
          )
        )
      )
    )
  );

  loadAllApplications$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ApplicationActions.loadAllApplications),
      switchMap(() =>
        this.applicationService.getAllApplications().pipe(
          map((applications) => ApplicationActions.loadAllApplicationsSuccess({ applications })),
          catchError((error) =>
            of(ApplicationActions.loadAllApplicationsFailure({ error: error.error?.message || 'Failed to load all applications.' }))
          )
        )
      )
    )
  );

  loadApplicationById$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ApplicationActions.loadApplicationById),
      switchMap(({ applicationId }) =>
        this.applicationService.getApplicationById(applicationId).pipe(
          map((application) => ApplicationActions.loadApplicationByIdSuccess({ application })),
          catchError((error) =>
            of(ApplicationActions.loadApplicationByIdFailure({ error: error.error?.message || 'Failed to load application details.' }))
          )
        )
      )
    )
  );

  createApplication$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ApplicationActions.createApplication),
      switchMap(({ request }) =>
        this.applicationService.createApplication(request).pipe(
          map((application) => ApplicationActions.createApplicationSuccess({ application })),
          catchError((error) =>
            of(ApplicationActions.createApplicationFailure({ error: error.error?.message || 'Failed to create application.' }))
          )
        )
      )
    )
  );

  createApplicationSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(ApplicationActions.createApplicationSuccess),
        tap(({ application }) => {
          this.notificationService.success('Draft Saved', 'Your delay cover application was initialized.');
          this.router.navigate(['/applications', application.applicationId]);
        })
      ),
    { dispatch: false }
  );

  submitApplication$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ApplicationActions.submitApplication),
      switchMap(({ applicationId }) =>
        this.applicationService.submitApplication(applicationId).pipe(
          map((application) => ApplicationActions.submitApplicationSuccess({ application })),
          catchError((error) =>
            of(ApplicationActions.submitApplicationFailure({ error: error.error?.message || 'Failed to submit application.' }))
          )
        )
      )
    )
  );

  submitApplicationSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(ApplicationActions.submitApplicationSuccess),
        tap(() => {
          this.notificationService.success('Submitted', 'Application submitted to verification queue.');
        })
      ),
    { dispatch: false }
  );

  withdrawApplication$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ApplicationActions.withdrawApplication),
      switchMap(({ applicationId }) =>
        this.applicationService.withdrawApplication(applicationId).pipe(
          map((application) => ApplicationActions.withdrawApplicationSuccess({ application })),
          catchError((error) =>
            of(ApplicationActions.withdrawApplicationFailure({ error: error.error?.message || 'Failed to withdraw application.' }))
          )
        )
      )
    )
  );

  loadDocuments$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ApplicationActions.loadDocuments),
      switchMap(({ applicationId }) =>
        this.applicationService.getDocuments(applicationId).pipe(
          map((documents) => ApplicationActions.loadDocumentsSuccess({ documents })),
          catchError((error) =>
            of(ApplicationActions.loadDocumentsFailure({ error: error.error?.message || 'Failed to load documents.' }))
          )
        )
      )
    )
  );

  uploadDocument$ = createEffect(() =>
    this.actions$.pipe(
      ofType(ApplicationActions.uploadDocument),
      switchMap(({ applicationId, request }) =>
        this.applicationService.uploadDocument(applicationId, request).pipe(
          map((document) => ApplicationActions.uploadDocumentSuccess({ document })),
          catchError((error) =>
            of(ApplicationActions.uploadDocumentFailure({ error: error.error?.message || 'Failed to upload document.' }))
          )
        )
      )
    )
  );
}
