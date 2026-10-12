export type QuoteStatus = 'GENERATED' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED';
export type PolicyStatus = 'GENERATED' | 'ACTIVE' | 'COMPLETED' | 'CLOSED' | 'CANCELLED';

export interface Quote {
  quoteId: string;
  applicationId: string;
  planName: string;
  premiumAmount: number;
  monthlyCompensation: number;
  maximumDelayMonths: number;
  maximumPayout: number;
  gracePeriodDays: number;
  expiryDate: string;
  status: QuoteStatus;
  createdAt: string;
}

export interface Policy {
  policyId: string;
  applicationId: string;
  quoteId: string;
  paymentId: string;
  policyNumber: string;
  coverageAmount: number;
  premiumAmount: number;
  policyStatus: PolicyStatus;
  startDate: string;
  endDate: string;
  createdAt: string;
}

export interface GenerateQuoteRequest {
  applicationId: string;
  baseCoverageAmount?: number;
  coverageAmount?: number;
  generatedBy?: string;
  planTypes?: string[];
}

export interface CreatePolicyRequest {
  quoteId: string;
  paymentId: string;
}

export interface PolicyState {
  quotes: Quote[];
  policies: Policy[];
  selectedQuote: Quote | null;
  selectedPolicy: Policy | null;
  loading: boolean;
  error: string | null;
}
