import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectAllAdminUsers, selectAdminLoading } from '../../state/admin.selectors';
import * as AdminActions from '../../state/admin.actions';
import { UserProfileResponse } from '../../../auth/state/auth.models';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit {
  users$: Observable<UserProfileResponse[]>;
  loading$: Observable<boolean>;

  constructor(private store: Store) {
    this.users$ = this.store.select(selectAllAdminUsers);
    this.loading$ = this.store.select(selectAdminLoading);
  }

  ngOnInit(): void {
    this.store.dispatch(AdminActions.loadAllUsers());
  }

  onToggleStatus(userId: string, currentStatus: string): void {
    const newStatus = currentStatus === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    this.store.dispatch(
      AdminActions.updateUserStatus({
        userId: Number(userId),
        request: {
          status: newStatus,
          reason: 'Administrative role moderation'
        }
      })
    );
  }
}
