import { ClaimState, Claim } from './claim.models';
import {
  selectAllClaims,
  selectSelectedClaim,
  selectClaimLoading,
  selectClaimError
} from './claim.selectors';

describe('Claim Selectors', () => {
  const mockClaim: Claim = {
    claimId: 'CLM-001',
    policyId: 'POL-001',
    applicationId: 'APP-001',
    reason: 'Construction delayed',
    claimedAmount: 150000,
    approvedAmount: 120000,
    eligibleDelayMonths: 5,
    status: 'DELAY_VERIFIED',
    remarks: 'Delay verified',
    createdAt: '2026-10-12T10:00:00'
  };

  const state: ClaimState = {
    claims: [mockClaim],
    selectedClaim: mockClaim,
    loading: false,
    error: 'Test error message'
  };

  it('selectAllClaims should return all claims', () => {
    const result = selectAllClaims.projector(state);
    expect(result).toEqual([mockClaim]);
  });

  it('selectSelectedClaim should return currently selected claim', () => {
    const result = selectSelectedClaim.projector(state);
    expect(result).toEqual(mockClaim);
  });

  it('selectClaimLoading should return loading status', () => {
    const result = selectClaimLoading.projector(state);
    expect(result).toBeFalse();
  });

  it('selectClaimError should return error message', () => {
    const result = selectClaimError.projector(state);
    expect(result).toBe('Test error message');
  });
});

