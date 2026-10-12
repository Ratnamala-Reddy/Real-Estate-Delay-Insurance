import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectClaimLoading, selectClaimError } from '../../state/claim.selectors';
import * as ClaimActions from '../../state/claim.actions';
import { PolicyService } from '../../../quotes-policies/services/policy.service';
import { Policy } from '../../../quotes-policies/state/policy.models';

@Component({
  selector: 'app-claim-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './claim-create.component.html',
  styleUrls: ['./claim-create.component.css']
})
export class ClaimCreateComponent implements OnInit {
  claimForm: FormGroup;
  loading$: Observable<boolean>;
  error$: Observable<string | null>;
  policies: Policy[] = [];
  selectedPolicy: Policy | null = null;
  loadingPolicies = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private store: Store,
    private policyService: PolicyService
  ) {
    this.claimForm = this.fb.group({
      policyId: ['', Validators.required],
      reason: ['', [Validators.required, Validators.minLength(10)]],
      claimedAmount: ['', [Validators.required, Validators.min(1)]],
      actualPossessionDate: ['']
    });
    this.loading$ = this.store.select(selectClaimLoading);
    this.error$ = this.store.select(selectClaimError);
  }

  ngOnInit(): void {
    const policyIdParam = this.route.snapshot.queryParams['policyId'];
    if (policyIdParam) {
      this.claimForm.patchValue({ policyId: policyIdParam });
    }
    this.loadPolicies(policyIdParam);
  }

  loadPolicies(policyIdParam?: string): void {
    this.loadingPolicies = true;
    this.policyService.getPolicies().subscribe({
      next: (list) => {
        this.loadingPolicies = false;
        this.policies = list.filter(p => p.policyStatus === 'ACTIVE');
        if (this.policies.length === 0 && list.length > 0) {
          this.policies = list;
        }

        if (policyIdParam) {
          const match = this.policies.find(p => p.policyId === policyIdParam || p.policyNumber === policyIdParam);
          if (match) {
            this.selectedPolicy = match;
            this.claimForm.patchValue({ policyId: match.policyId });
          }
        } else if (this.policies.length === 1) {
          this.selectedPolicy = this.policies[0];
          this.claimForm.patchValue({ policyId: this.policies[0].policyId });
        }
      },
      error: () => {
        this.loadingPolicies = false;
      }
    });
  }

  onPolicyChange(event: Event): void {
    const selPolicyId = (event.target as HTMLSelectElement).value;
    const match = this.policies.find(p => p.policyId === selPolicyId);
    this.selectedPolicy = match || null;
    this.claimForm.patchValue({ policyId: selPolicyId });
  }

  onSubmit(): void {
    if (this.claimForm.valid) {
      const val = this.claimForm.value;
      this.store.dispatch(
        ClaimActions.createClaim({
          request: {
            policyId: (val.policyId || '').trim(),
            reason: (val.reason || '').trim(),
            claimedAmount: Number(val.claimedAmount),
            actualPossessionDate: val.actualPossessionDate ? val.actualPossessionDate : undefined
          }
        })
      );
    } else {
      this.claimForm.markAllAsTouched();
    }
  }
}
