import { createAction, props } from '@ngrx/store';
import {
  Quote,
  Policy,
  GenerateQuoteRequest,
  CreatePolicyRequest
} from './policy.models';

export const generateQuotes = createAction(
  '[Policy] Generate Quotes',
  props<{ applicationId: string; request: GenerateQuoteRequest }>()
);
export const generateQuotesSuccess = createAction(
  '[Policy] Generate Quotes Success',
  props<{ quotes: Quote[] }>()
);
export const generateQuotesFailure = createAction(
  '[Policy] Generate Quotes Failure',
  props<{ error: string }>()
);

export const loadQuotes = createAction(
  '[Policy] Load Quotes',
  props<{ applicationId: string }>()
);
export const loadQuotesSuccess = createAction(
  '[Policy] Load Quotes Success',
  props<{ quotes: Quote[] }>()
);
export const loadQuotesFailure = createAction(
  '[Policy] Load Quotes Failure',
  props<{ error: string }>()
);

export const acceptQuote = createAction(
  '[Policy] Accept Quote',
  props<{ quoteId: string }>()
);
export const acceptQuoteSuccess = createAction(
  '[Policy] Accept Quote Success',
  props<{ quote: Quote }>()
);
export const acceptQuoteFailure = createAction(
  '[Policy] Accept Quote Failure',
  props<{ error: string }>()
);

export const declineQuote = createAction(
  '[Policy] Decline Quote',
  props<{ quoteId: string }>()
);
export const declineQuoteSuccess = createAction(
  '[Policy] Decline Quote Success',
  props<{ quote: Quote }>()
);
export const declineQuoteFailure = createAction(
  '[Policy] Decline Quote Failure',
  props<{ error: string }>()
);

export const loadPolicies = createAction('[Policy] Load Policies');
export const loadPoliciesSuccess = createAction(
  '[Policy] Load Policies Success',
  props<{ policies: Policy[] }>()
);
export const loadPoliciesFailure = createAction(
  '[Policy] Load Policies Failure',
  props<{ error: string }>()
);

export const loadPolicyById = createAction(
  '[Policy] Load Policy By ID',
  props<{ policyId: string }>()
);
export const loadPolicyByIdSuccess = createAction(
  '[Policy] Load Policy By ID Success',
  props<{ policy: Policy }>()
);
export const loadPolicyByIdFailure = createAction(
  '[Policy] Load Policy By ID Failure',
  props<{ error: string }>()
);

export const createPolicy = createAction(
  '[Policy] Create Policy',
  props<{ request: CreatePolicyRequest }>()
);
export const createPolicySuccess = createAction(
  '[Policy] Create Policy Success',
  props<{ policy: Policy }>()
);
export const createPolicyFailure = createAction(
  '[Policy] Create Policy Failure',
  props<{ error: string }>()
);
