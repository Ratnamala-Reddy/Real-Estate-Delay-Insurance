import { createReducer, on } from '@ngrx/store';
import { ClaimState } from './claim.models';
import * as ClaimActions from './claim.actions';

export const initialClaimState: ClaimState = {
  claims: [],
  selectedClaim: null,
  loading: false,
  error: null
};

export const claimReducer = createReducer(
  initialClaimState,
  on(
    ClaimActions.loadClaims,
    ClaimActions.loadClaimById,
    ClaimActions.createClaim,
    ClaimActions.calculateDelay,
    ClaimActions.calculateCompensation,
    ClaimActions.verifyInsuranceEligibility,
    ClaimActions.approveClaim,
    ClaimActions.rejectClaim,
    ClaimActions.closeClaim,
    (state) => ({ ...state, loading: true, error: null })
  ),
  on(ClaimActions.loadClaimsSuccess, (state, { claims }) => ({
    ...state,
    loading: false,
    claims,
    error: null
  })),
  on(ClaimActions.loadClaimByIdSuccess, ClaimActions.createClaimSuccess, ClaimActions.updateClaimSuccess, (state, { claim }) => ({
    ...state,
    loading: false,
    selectedClaim: claim,
    claims: state.claims.some((c) => c.claimId === claim.claimId)
      ? state.claims.map((c) => (c.claimId === claim.claimId ? claim : c))
      : [claim, ...state.claims],
    error: null
  })),
  on(
    ClaimActions.loadClaimsFailure,
    ClaimActions.loadClaimByIdFailure,
    ClaimActions.createClaimFailure,
    ClaimActions.updateClaimFailure,
    (state, { error }) => ({
      ...state,
      loading: false,
      error
    })
  )
);
