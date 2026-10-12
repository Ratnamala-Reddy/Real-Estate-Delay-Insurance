import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable, combineLatest, map } from 'rxjs';
import { selectVerificationQueue, selectVerificationLoading } from '../../state/verification.selectors';
import * as VerificationActions from '../../state/verification.actions';
import * as ApplicationActions from '../../../applications/state/application.actions';
import { selectAllApplications, selectApplicationLoading } from '../../../applications/state/application.selectors';
import { Application } from '../../../applications/state/application.models';
import { AdminService } from '../../../admin/services/admin.service';
import { UserProfileResponse } from '../../../auth/state/auth.models';
import { NotificationService } from '../../../../core/services/notification.service';

export type QueueTab = 'ALL' | 'PENDING' | 'VERIFIED' | 'DRAFT';

@Component({
  selector: 'app-verification-queue',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './verification-queue.component.html',
  styleUrls: ['./verification-queue.component.css']
})
export class VerificationQueueComponent implements OnInit {
  activeTab: QueueTab = 'ALL';
  applications$: Observable<Application[]>;
  filteredApplications$: Observable<Application[]>;
  loading$: Observable<boolean>;

  totalCount$: Observable<number>;
  pendingCount$: Observable<number>;
  verifiedCount$: Observable<number>;
  draftCount$: Observable<number>;

  usersMap = new Map<number, UserProfileResponse>();
  pendingUsersCount = 0;

  constructor(
    private store: Store,
    private adminService: AdminService,
    private notificationService: NotificationService
  ) {
    this.applications$ = this.store.select(selectAllApplications);
    const appLoading$ = this.store.select(selectApplicationLoading);
    const verifLoading$ = this.store.select(selectVerificationLoading);

    this.loading$ = combineLatest([appLoading$, verifLoading$]).pipe(
      map(([a, v]) => a || v)
    );

    this.totalCount$ = this.applications$.pipe(map(apps => apps.length));
    this.pendingCount$ = this.applications$.pipe(
      map(apps => apps.filter(a => a.status === 'SUBMITTED' || a.status === 'VERIFICATION_IN_PROGRESS' || a.status === 'ADDITIONAL_INFORMATION_REQUIRED').length)
    );
    this.verifiedCount$ = this.applications$.pipe(
      map(apps => apps.filter(a => a.status === 'VERIFIED').length)
    );
    this.draftCount$ = this.applications$.pipe(
      map(apps => apps.filter(a => a.status === 'DRAFT').length)
    );

    this.filteredApplications$ = combineLatest([this.applications$, this.store.select(state => this.activeTab)]).pipe(
      map(([apps]) => {
        if (this.activeTab === 'PENDING') {
          return apps.filter(a => a.status === 'SUBMITTED' || a.status === 'VERIFICATION_IN_PROGRESS' || a.status === 'ADDITIONAL_INFORMATION_REQUIRED');
        } else if (this.activeTab === 'VERIFIED') {
          return apps.filter(a => a.status === 'VERIFIED');
        } else if (this.activeTab === 'DRAFT') {
          return apps.filter(a => a.status === 'DRAFT');
        }
        return apps;
      })
    );
  }

  ngOnInit(): void {
    this.store.dispatch(VerificationActions.loadVerificationQueue());
    this.store.dispatch(ApplicationActions.loadAllApplications());
    this.loadUsers();
  }

  loadUsers(): void {
    this.adminService.getAllUsers().subscribe({
      next: (users) => {
        this.usersMap.clear();
        users.forEach((u) => {
          if (u.userId) {
            this.usersMap.set(Number(u.userId), u);
          }
        });
        this.computePendingCount();
      },
      error: () => {}
    });
  }

  computePendingCount(): void {
    this.applications$.subscribe((apps) => {
      let count = 0;
      apps.forEach((app) => {
        const u = this.usersMap.get(app.userId);
        if (u && u.status === 'PENDING_VERIFICATION') {
          count++;
        }
      });
      this.pendingUsersCount = count;
    });
  }

  getUser(userId: number): UserProfileResponse | undefined {
    return this.usersMap.get(userId);
  }

  isUserPending(userId: number): boolean {
    const user = this.usersMap.get(userId);
    return user ? user.status === 'PENDING_VERIFICATION' : false;
  }

  onActivateUser(userId: number, event: Event): void {
    event.stopPropagation();
    this.adminService.updateUserStatus(userId, { status: 'ACTIVE', reason: 'Activated for underwriting verification' }).subscribe({
      next: () => {
        this.notificationService.success('User Activated', `User #${userId} account is now ACTIVE and verified.`);
        this.loadUsers();
      },
      error: (err) => {
        this.notificationService.error('Activation Failed', err?.error?.message || 'Could not activate user.');
      }
    });
  }

  setTab(tab: QueueTab): void {
    this.activeTab = tab;
    this.filteredApplications$ = this.applications$.pipe(
      map(apps => {
        if (this.activeTab === 'PENDING') {
          return apps.filter(a => a.status === 'SUBMITTED' || a.status === 'VERIFICATION_IN_PROGRESS' || a.status === 'ADDITIONAL_INFORMATION_REQUIRED');
        } else if (this.activeTab === 'VERIFIED') {
          return apps.filter(a => a.status === 'VERIFIED');
        } else if (this.activeTab === 'DRAFT') {
          return apps.filter(a => a.status === 'DRAFT');
        }
        return apps;
      })
    );
  }
}
