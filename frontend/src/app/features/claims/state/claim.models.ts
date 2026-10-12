export type ClaimStatus =
  | 'DRAFT'
  | 'DOCUMENTS_SUBMITTED'
  | 'DELAY_VERIFIED'
  | 'ELIGIBILITY_VERIFIED'
  | 'APPROVED'
  | 'REJECTED'
  | 'SETTLED'
  | 'CLOSED';

export interface CreateClaimRequest {
  policyId: string;
  reason: string;
  claimedAmount: number;
  actualPossessionDate?: string;
}

export interface Claim {
  claimId: string;
  policyId: string;
  applicationId: string;
  reason: string;
  claimedAmount: number;
  approvedAmount: number;
  eligibleDelayMonths: number;
  status: ClaimStatus;
  remarks: string;
  createdAt: string;
  actualPossessionDate?: string;
}

export interface ClaimState {
  claims: Claim[];
  selectedClaim: Claim | null;
  loading: boolean;
  error: string | null;
}
