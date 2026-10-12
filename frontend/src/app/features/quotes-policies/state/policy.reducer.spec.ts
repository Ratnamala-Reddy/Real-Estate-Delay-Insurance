import { policyReducer, initialPolicyState } from './policy.reducer';
import * as PolicyActions from './policy.actions';
import { Policy, Quote } from './policy.models';

describe('Policy Reducer', () => {
  const mockQuote: Quote = {
    quoteId: 'QUO-001',
    applicationId: 'APP-001',
    planName: 'STANDARD',
    premiumAmount: 18500,
    monthlyCompensation: 50000,
    maximumDelayMonths: 12,
    maximumPayout: 600000,
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
    policyNumber: 'POL-ABCD1234',
    coverageAmount: 5000000,
    premiumAmount: 18500,
    policyStatus: 'ACTIVE',
    startDate: '2026-10-12',
    endDate: '2028-10-12',
    createdAt: '2026-10-12T10:00:00'
  };

  it('should set loading to true on generateQuotes action', () => {
    const state = policyReducer(
      initialPolicyState,
      PolicyActions.generateQuotes({ applicationId: 'APP-001', request: { applicationId: 'APP-001', coverageAmount: 5000000 } })
    );
    expect(state.loading).toBeTrue();
    expect(state.error).toBeNull();
  });

  it('should store quotes on generateQuotesSuccess', () => {
    const state = policyReducer(
      { ...initialPolicyState, loading: true },
      PolicyActions.generateQuotesSuccess({ quotes: [mockQuote] })
    );
    expect(state.loading).toBeFalse();
    expect(state.quotes).toEqual([mockQuote]);
  });

  it('should store policies on loadPoliciesSuccess', () => {
    const state = policyReducer(
      { ...initialPolicyState, loading: true },
      PolicyActions.loadPoliciesSuccess({ policies: [mockPolicy] })
    );
    expect(state.loading).toBeFalse();
    expect(state.policies).toEqual([mockPolicy]);
  });

  it('should set selectedPolicy on createPolicySuccess', () => {
    const state = policyReducer(
      initialPolicyState,
      PolicyActions.createPolicySuccess({ policy: mockPolicy })
    );
    expect(state.loading).toBeFalse();
    expect(state.selectedPolicy).toEqual(mockPolicy);
    expect(state.policies.length).toBe(1);
  });
});

