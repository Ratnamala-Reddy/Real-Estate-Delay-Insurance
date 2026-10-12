import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectCurrentVerification, selectVerificationLoading } from '../../state/verification.selectors';
import * as VerificationActions from '../../state/verification.actions';
import { VerificationRecord } from '../../state/verification.models';
import { ApplicationService } from '../../../applications/services/application.service';
import { AdminService } from '../../../admin/services/admin.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { UserProfileResponse } from '../../../auth/state/auth.models';
import { Application } from '../../../applications/state/application.models';

@Component({
  selector: 'app-verification-process',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './verification-process.component.html',
  styleUrls: ['./verification-process.component.css']
})
export class VerificationProcessComponent implements OnInit {
  applicationId!: string;
  verification$: Observable<VerificationRecord | null>;
  loading$: Observable<boolean>;
  actionForm: FormGroup;

  application: Application | null = null;
  applicantUser: UserProfileResponse | null = null;
  isApplicantPending = false;
  activatingUser = false;

  constructor(
    private route: ActivatedRoute,
    private store: Store,
    private fb: FormBuilder,
    private applicationService: ApplicationService,
    private adminService: AdminService,
    private notificationService: NotificationService
  ) {
    this.verification$ = this.store.select(selectCurrentVerification);
    this.loading$ = this.store.select(selectVerificationLoading);

    this.actionForm = this.fb.group({
      remarks: ['', [Validators.required, Validators.minLength(5)]],
      officer: ['Underwriter Admin', Validators.required]
    });
  }

  ngOnInit(): void {
    this.applicationId = this.route.snapshot.paramMap.get('id') || '';
    if (this.applicationId) {
      this.store.dispatch(VerificationActions.loadVerificationResult({ applicationId: this.applicationId }));
      this.loadApplicantInfo();
    }
  }

  loadApplicantInfo(): void {
    this.applicationService.getApplicationById(this.applicationId).subscribe({
      next: (app) => {
        this.application = app;
        if (app && app.userId) {
          this.adminService.getAllUsers().subscribe({
            next: (users) => {
              const u = users.find((user) => Number(user.userId) === Number(app.userId));
              if (u) {
                this.applicantUser = u;
                this.isApplicantPending = (u.status === 'PENDING_VERIFICATION');
              }
            },
            error: () => {}
          });
        }
      },
      error: () => {}
    });
  }

  onActivateApplicant(): void {
    if (!this.applicantUser || !this.applicantUser.userId) return;
    this.activatingUser = true;

    this.adminService.updateUserStatus(Number(this.applicantUser.userId), {
      status: 'ACTIVE',
      reason: 'Activated for underwriting approval'
    }).subscribe({
      next: () => {
        this.activatingUser = false;
        this.isApplicantPending = false;
        if (this.applicantUser) {
          this.applicantUser.status = 'ACTIVE';
        }
        this.notificationService.success('User Activated', `Applicant ${this.applicantUser?.email} account is now ACTIVE and verified.`);
      },
      error: (err) => {
        this.activatingUser = false;
        this.notificationService.error('Activation Failed', err?.error?.message || 'Failed to activate user account.');
      }
    });
  }

  onVerifyProperty(): void {
    if (this.actionForm.valid) {
      this.store.dispatch(
        VerificationActions.verifyProperty({
          applicationId: this.applicationId,
          request: {
            remarks: this.actionForm.value.remarks,
            verifiedBy: this.actionForm.value.officer
          }
        })
      );
    }
  }

  onRequestInfo(): void {
    if (this.actionForm.valid) {
      this.store.dispatch(
        VerificationActions.requestAdditionalInfo({
          applicationId: this.applicationId,
          request: {
            remarks: this.actionForm.value.remarks,
            requestedBy: this.actionForm.value.officer
          }
        })
      );
    }
  }

  onApprove(): void {
    if (this.isApplicantPending) {
      this.notificationService.warning(
        'User Activation Required',
        'This applicant account is still PENDING_VERIFICATION. Please click "Activate User Now" before approving underwriting.'
      );
      return;
    }

    if (this.actionForm.valid) {
      this.store.dispatch(
        VerificationActions.approveVerification({
          applicationId: this.applicationId,
          request: {
            remarks: this.actionForm.value.remarks,
            approvedBy: this.actionForm.value.officer
          }
        })
      );
    }
  }

  onReject(): void {
    if (this.actionForm.valid) {
      this.store.dispatch(
        VerificationActions.rejectVerification({
          applicationId: this.applicationId,
          request: {
            remarks: this.actionForm.value.remarks,
            rejectedBy: this.actionForm.value.officer
          }
        })
      );
    }
  }
}
