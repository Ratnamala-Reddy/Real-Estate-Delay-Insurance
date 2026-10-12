import { claimReducer, initialClaimState } from './claim.reducer';
import * as ClaimActions from './claim.actions';
import { Claim } from './claim.models';

describe('Claim Reducer', () => {
  const mockClaim: Claim = {
    claimId: 'CLM-001',
    policyId: 'POL-001',
    applicationId: 'APP-001',
    reason: 'Construction delayed by 6 months',
    claimedAmount: 120000,
    approvedAmount: 100000,
    eligibleDelayMonths: 4,
    status: 'DOCUMENTS_SUBMITTED',
    remarks: 'Submitted proof of delay',
    createdAt: '2026-10-12T10:00:00'
  };

  it('should return initial state when unknown action passed', () => {
    const action = { type: 'NOOP' } as any;
    const state = claimReducer(undefined, action);
    expect(state).toEqual(initialClaimState);
  });

  it('should set loading to true on loadClaims action', () => {
    const state = claimReducer(initialClaimState, ClaimActions.loadClaims());
    expect(state.loading).toBeTrue();
    expect(state.error).toBeNull();
  });

  it('should populate claims and set loading to false on loadClaimsSuccess', () => {
    const state = claimReducer(
      { ...initialClaimState, loading: true },
      ClaimActions.loadClaimsSuccess({ claims: [mockClaim] })
    );
    expect(state.loading).toBeFalse();
    expect(state.claims).toEqual([mockClaim]);
    expect(state.error).toBeNull();
  });

  it('should add or update selected claim on createClaimSuccess', () => {
    const state = claimReducer(
      initialClaimState,
      ClaimActions.createClaimSuccess({ claim: mockClaim })
    );
    expect(state.selectedClaim).toEqual(mockClaim);
    expect(state.claims.length).toBe(1);
    expect(state.claims[0]).toEqual(mockClaim);
  });

  it('should set error on createClaimFailure', () => {
    const state = claimReducer(
      { ...initialClaimState, loading: true },
      ClaimActions.createClaimFailure({ error: 'Claim submission failed' })
    );
    expect(state.loading).toBeFalse();
    expect(state.error).toBe('Claim submission failed');
  });
});

