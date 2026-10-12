import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { AuthService } from '../services/auth.service';
import * as AuthActions from './auth.actions';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { Router } from '@angular/router';
import { NotificationService } from '../../../core/services/notification.service';

@Injectable()
export class AuthEffects {
  constructor(
    private actions$: Actions,
    private authService: AuthService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.login),
      switchMap(({ request, returnUrl }) =>
        this.authService.login(request).pipe(
          map((response) => AuthActions.loginSuccess({ response, returnUrl })),
          catchError((error) =>
            of(AuthActions.loginFailure({ error: error.error?.message || 'Login failed. Check credentials.' }))
          )
        )
      )
    )
  );

  loginSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.loginSuccess),
        tap(({ response, returnUrl }) => {
          this.notificationService.success('Welcome back', response.message || 'Login successful');
          if (returnUrl) {
            this.router.navigateByUrl(returnUrl);
          } else if (response.role === 'ADMIN') {
            this.router.navigate(['/admin/dashboard']);
          } else {
            this.router.navigate(['/applications']);
          }
        })
      ),
    { dispatch: false }
  );

  register$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.register),
      switchMap(({ request }) =>
        this.authService.register(request).pipe(
          map((response) => AuthActions.registerSuccess({ response })),
          catchError((error) =>
            of(AuthActions.registerFailure({ error: error.error?.message || 'Registration failed.' }))
          )
        )
      )
    )
  );

  registerSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.registerSuccess),
        tap(({ response }) => {
          this.notificationService.success('Registration Complete', response.message || 'Account created successfully.');
          this.router.navigate(['/auth/login']);
        })
      ),
    { dispatch: false }
  );

  loadProfile$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.loadProfile),
      switchMap(() =>
        this.authService.getCurrentUser().pipe(
          map((profile) => AuthActions.loadProfileSuccess({ profile })),
          catchError((error) =>
            of(AuthActions.loadProfileFailure({ error: error.error?.message || 'Failed to load profile.' }))
          )
        )
      )
    )
  );

  updateProfile$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.updateProfile),
      switchMap(({ request }) =>
        this.authService.updateProfile(request).pipe(
          map((profile) => AuthActions.updateProfileSuccess({ profile })),
          catchError((error) =>
            of(AuthActions.updateProfileFailure({ error: error.error?.message || 'Failed to update profile.' }))
          )
        )
      )
    )
  );

  updateProfileSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.updateProfileSuccess),
        tap(() => {
          this.notificationService.success('Profile Updated', 'Your profile details have been saved.');
        })
      ),
    { dispatch: false }
  );

  logout$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.logout),
      switchMap(() =>
        this.authService.logout().pipe(
          map(() => AuthActions.logoutSuccess()),
          catchError(() => of(AuthActions.logoutSuccess()))
        )
      )
    )
  );

  logoutSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.logoutSuccess),
        tap(() => {
          this.notificationService.info('Signed Out', 'You have been safely signed out.');
          this.router.navigate(['/auth/login']);
        })
      ),
    { dispatch: false }
  );
}
