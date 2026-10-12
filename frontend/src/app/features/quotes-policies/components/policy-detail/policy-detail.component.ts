import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectSelectedPolicy, selectPolicyLoading } from '../../state/policy.selectors';
import * as PolicyActions from '../../state/policy.actions';
import { Policy } from '../../state/policy.models';
import { PolicyService } from '../../services/policy.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { selectUserRole } from '../../../auth/state/auth.selectors';

export interface PolicyEndorsement {
  id: string;
  type: string;
  details: string;
  status: 'PENDING' | 'APPROVED' | 'EFFECTIVE';
  effectiveDate: string;
}

@Component({
  selector: 'app-policy-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './policy-detail.component.html',
  styleUrls: ['./policy-detail.component.css']
})
export class PolicyDetailComponent implements OnInit {
  policyId!: string;
  policy$: Observable<Policy | null>;
  loading$: Observable<boolean>;
  userRole$: Observable<string | null>;

  activeTab: 'certificate' | 'servicing' | 'lifecycle' | 'manage' = 'certificate';

  endorsementForm: FormGroup;
  endorsements: PolicyEndorsement[] = [];
  showCancelModal = false;
  showRenewModal = false;
  actionLoading = false;

  constructor(
    private route: ActivatedRoute,
    private store: Store,
    private fb: FormBuilder,
    private policyService: PolicyService,
    private notificationService: NotificationService
  ) {
    this.policy$ = this.store.select(selectSelectedPolicy);
    this.loading$ = this.store.select(selectPolicyLoading);
    this.userRole$ = this.store.select(selectUserRole);

    this.endorsementForm = this.fb.group({
      endorsementType: ['ADDRESS_CHANGE', Validators.required],
      details: ['', [Validators.required, Validators.minLength(10)]]
    });
  }

  ngOnInit(): void {
    this.policyId = this.route.snapshot.paramMap.get('id') || '';
    if (this.policyId) {
      this.store.dispatch(PolicyActions.loadPolicyById({ policyId: this.policyId }));
      this.loadEndorsements();
    }
  }

  loadEndorsements(): void {
    const saved = localStorage.getItem(`endorsements_${this.policyId}`);
    if (saved) {
      try {
        this.endorsements = JSON.parse(saved);
      } catch (e) {
        this.endorsements = [];
      }
    } else {
      this.endorsements = [
        {
          id: 'END-001',
          type: 'Initial Binding & Escrow Assignment',
          details: 'Standard statutory delay indemnity bond underwritten and active.',
          status: 'EFFECTIVE',
          effectiveDate: new Date().toISOString()
        }
      ];
    }
  }

  saveEndorsements(): void {
    localStorage.setItem(`endorsements_${this.policyId}`, JSON.stringify(this.endorsements));
  }

  onSubmitEndorsement(): void {
    if (this.endorsementForm.invalid) {
      this.endorsementForm.markAllAsTouched();
      return;
    }

    const val = this.endorsementForm.value;
    const newEndorsement: PolicyEndorsement = {
      id: `END-${Date.now().toString().slice(-4)}`,
      type: val.endorsementType.replace('_', ' '),
      details: val.details,
      status: 'APPROVED',
      effectiveDate: new Date().toISOString()
    };

    this.endorsements.unshift(newEndorsement);
    this.saveEndorsements();
    this.endorsementForm.reset({ endorsementType: 'ADDRESS_CHANGE' });
    this.notificationService.success('Policy Servicing', 'Policy servicing endorsement recorded successfully.');
  }

  openCancelModal(): void {
    this.showCancelModal = true;
  }

  closeCancelModal(): void {
    this.showCancelModal = false;
  }

  confirmCancel(): void {
    this.actionLoading = true;
    this.policyService.closePolicy(this.policyId).subscribe({
      next: () => {
        this.actionLoading = false;
        this.showCancelModal = false;
        this.notificationService.success('Policy Closed', 'Policy has been successfully closed/cancelled.');
        this.store.dispatch(PolicyActions.loadPolicyById({ policyId: this.policyId }));
      },
      error: (err) => {
        this.actionLoading = false;
        this.showCancelModal = false;
        this.notificationService.error('Closure Error', err?.error?.message || 'Failed to close policy');
      }
    });
  }

  openRenewModal(): void {
    this.showRenewModal = true;
  }

  closeRenewModal(): void {
    this.showRenewModal = false;
  }

  confirmRenew(): void {
    this.actionLoading = true;
    const renewEndorsement: PolicyEndorsement = {
      id: `REN-${Date.now().toString().slice(-4)}`,
      type: 'Policy Term Renewal & Extension',
      details: 'Handover delay coverage period extended by +12 months under active underwriting rules.',
      status: 'EFFECTIVE',
      effectiveDate: new Date().toISOString()
    };
    this.endorsements.unshift(renewEndorsement);
    this.saveEndorsements();

    setTimeout(() => {
      this.actionLoading = false;
      this.showRenewModal = false;
      this.notificationService.success('Term Extended', 'Policy term renewal extended successfully.');
    }, 600);
  }
}
