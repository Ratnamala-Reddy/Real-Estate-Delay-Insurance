import { Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { AdminService } from '../services/admin.service';
import * as AdminActions from './admin.actions';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';
import { NotificationService } from '../../../core/services/notification.service';

@Injectable()
export class AdminEffects {
  constructor(
    private actions$: Actions,
    private adminService: AdminService,
    private notificationService: NotificationService
  ) {}

  loadUsers$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AdminActions.loadAllUsers),
      switchMap(() =>
        this.adminService.getAllUsers().pipe(
          map((users) => AdminActions.loadAllUsersSuccess({ users })),
          catchError((error) =>
            of(AdminActions.loadAllUsersFailure({ error: error.error?.message || 'Failed to load users.' }))
          )
        )
      )
    )
  );

  updateStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AdminActions.updateUserStatus),
      switchMap(({ userId, request }) =>
        this.adminService.updateUserStatus(userId, request).pipe(
          map(() => AdminActions.updateUserStatusSuccess({ userId, status: request.status })),
          catchError((error) =>
            of(AdminActions.updateUserStatusFailure({ error: error.error?.message || 'Failed to update user status.' }))
          )
        )
      )
    )
  );

  updateStatusSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AdminActions.updateUserStatusSuccess),
        tap(({ status }) => {
          this.notificationService.success('Status Updated', `User account marked as ${status}.`);
        })
      ),
    { dispatch: false }
  );
}
