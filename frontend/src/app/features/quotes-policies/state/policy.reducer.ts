import { createReducer, on } from '@ngrx/store';
import { PolicyState } from './policy.models';
import * as PolicyActions from './policy.actions';

export const initialPolicyState: PolicyState = {
  quotes: [],
  policies: [],
  selectedQuote: null,
  selectedPolicy: null,
  loading: false,
  error: null
};

export const policyReducer = createReducer(
  initialPolicyState,
  on(
    PolicyActions.generateQuotes,
    PolicyActions.loadQuotes,
    PolicyActions.acceptQuote,
    PolicyActions.declineQuote,
    PolicyActions.loadPolicies,
    PolicyActions.loadPolicyById,
    PolicyActions.createPolicy,
    (state) => ({ ...state, loading: true, error: null })
  ),
  on(PolicyActions.generateQuotesSuccess, PolicyActions.loadQuotesSuccess, (state, { quotes }) => ({
    ...state,
    loading: false,
    quotes,
    error: null
  })),
  on(PolicyActions.acceptQuoteSuccess, PolicyActions.declineQuoteSuccess, (state, { quote }) => ({
    ...state,
    loading: false,
    selectedQuote: quote,
    quotes: state.quotes.map((q) => (q.quoteId === quote.quoteId ? quote : q)),
    error: null
  })),
  on(PolicyActions.loadPoliciesSuccess, (state, { policies }) => ({
    ...state,
    loading: false,
    policies,
    error: null
  })),
  on(PolicyActions.loadPolicyByIdSuccess, PolicyActions.createPolicySuccess, (state, { policy }) => ({
    ...state,
    loading: false,
    selectedPolicy: policy,
    policies: state.policies.some((p) => p.policyId === policy.policyId)
      ? state.policies.map((p) => (p.policyId === policy.policyId ? policy : p))
      : [policy, ...state.policies],
    error: null
  })),
  on(
    PolicyActions.generateQuotesFailure,
    PolicyActions.loadQuotesFailure,
    PolicyActions.acceptQuoteFailure,
    PolicyActions.declineQuoteFailure,
    PolicyActions.loadPoliciesFailure,
    PolicyActions.loadPolicyByIdFailure,
    PolicyActions.createPolicyFailure,
    (state, { error }) => ({
      ...state,
      loading: false,
      error
    })
  )
);
