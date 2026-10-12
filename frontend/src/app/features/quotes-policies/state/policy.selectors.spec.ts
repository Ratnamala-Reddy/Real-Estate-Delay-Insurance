import { PolicyState, Policy, Quote } from './policy.models';
import {
  selectAllQuotes,
  selectAllPolicies,
  selectSelectedPolicy,
  selectPolicyLoading,
  selectPolicyError
} from './policy.selectors';

describe('Policy Selectors', () => {
  const mockQuote: Quote = {
    quoteId: 'QUO-001',
    applicationId: 'APP-001',
    planName: 'BASIC',
    premiumAmount: 8500,
    monthlyCompensation: 25000,
    maximumDelayMonths: 6,
    maximumPayout: 150000,
    gracePeriodDays: 30,
    expiryDate: '2026-11-12',
    status: 'GENERATED',
    createdAt: '2026-10-12T10:00:00'
  };

  const mockPolicy: Policy = {
    policyId: 'POL-001',
    applicationId: 'APP-001',
    quoteId: 'QUO-001',
    paymentId: 'PAY-001',
    policyNumber: 'POL-XYZ123',
    coverageAmount: 2000000,
    premiumAmount: 8500,
    policyStatus: 'ACTIVE',
    startDate: '2026-10-12',
    endDate: '2028-10-12',
    createdAt: '2026-10-12T10:00:00'
  };

  const state: PolicyState = {
    quotes: [mockQuote],
    policies: [mockPolicy],
    selectedQuote: mockQuote,
    selectedPolicy: mockPolicy,
    loading: false,
    error: null
  };

  it('selectAllQuotes should return all quotes', () => {
    expect(selectAllQuotes.projector(state)).toEqual([mockQuote]);
  });

  it('selectAllPolicies should return all policies', () => {
    expect(selectAllPolicies.projector(state)).toEqual([mockPolicy]);
  });

  it('selectSelectedPolicy should return selected policy', () => {
    expect(selectSelectedPolicy.projector(state)).toEqual(mockPolicy);
  });

  it('selectPolicyLoading should return loading status', () => {
    expect(selectPolicyLoading.projector(state)).toBeFalse();
  });

  it('selectPolicyError should return error', () => {
    expect(selectPolicyError.projector(state)).toBeNull();
  });
});

