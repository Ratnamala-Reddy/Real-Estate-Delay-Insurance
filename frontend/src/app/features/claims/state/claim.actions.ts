import { createAction, props } from '@ngrx/store';
import { Claim, CreateClaimRequest } from './claim.models';

export const loadClaims = createAction('[Claim] Load Claims');
export const loadClaimsSuccess = createAction(
  '[Claim] Load Claims Success',
  props<{ claims: Claim[] }>()
);
export const loadClaimsFailure = createAction(
  '[Claim] Load Claims Failure',
  props<{ error: string }>()
);

export const loadClaimById = createAction(
  '[Claim] Load Claim By ID',
  props<{ claimId: string }>()
);
export const loadClaimByIdSuccess = createAction(
  '[Claim] Load Claim By ID Success',
  props<{ claim: Claim }>()
);
export const loadClaimByIdFailure = createAction(
  '[Claim] Load Claim By ID Failure',
  props<{ error: string }>()
);

export const createClaim = createAction(
  '[Claim] Create Claim',
  props<{ request: CreateClaimRequest }>()
);
export const createClaimSuccess = createAction(
  '[Claim] Create Claim Success',
  props<{ claim: Claim }>()
);
export const createClaimFailure = createAction(
  '[Claim] Create Claim Failure',
  props<{ error: string }>()
);

export const calculateDelay = createAction(
  '[Claim] Calculate Delay',
  props<{ claimId: string }>()
);
export const calculateCompensation = createAction(
  '[Claim] Calculate Compensation',
  props<{ claimId: string }>()
);
export const verifyInsuranceEligibility = createAction(
  '[Claim] Verify Eligibility',
  props<{ claimId: string }>()
);
export const approveClaim = createAction(
  '[Claim] Approve Claim',
  props<{ claimId: string }>()
);
export const rejectClaim = createAction(
  '[Claim] Reject Claim',
  props<{ claimId: string }>()
);
export const closeClaim = createAction(
  '[Claim] Close Claim',
  props<{ claimId: string }>()
);

export const updateClaimSuccess = createAction(
  '[Claim] Update Claim Success',
  props<{ claim: Claim }>()
);
export const updateClaimFailure = createAction(
  '[Claim] Update Claim Failure',
  props<{ error: string }>()
);
