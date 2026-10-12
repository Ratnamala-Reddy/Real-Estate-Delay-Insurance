import { createReducer, on } from '@ngrx/store';
import { PaymentState } from './payment.models';
import * as PaymentActions from './payment.actions';

export const initialPaymentState: PaymentState = {
  currentPremiumPayment: null,
  compensationPayments: [],
  loading: false,
  error: null
};

export const paymentReducer = createReducer(
  initialPaymentState,
  on(
    PaymentActions.createPremiumPayment,
    PaymentActions.loadPremiumPayment,
    PaymentActions.createCompensationPayment,
    (state) => ({ ...state, loading: true, error: null })
  ),
  on(PaymentActions.createPremiumPaymentSuccess, PaymentActions.loadPremiumPaymentSuccess, (state, { payment }) => ({
    ...state,
    loading: false,
    currentPremiumPayment: payment,
    error: null
  })),
  on(PaymentActions.createCompensationPaymentSuccess, (state, { payment }) => ({
    ...state,
    loading: false,
    compensationPayments: [...state.compensationPayments, payment],
    error: null
  })),
  on(
    PaymentActions.createPremiumPaymentFailure,
    PaymentActions.loadPremiumPaymentFailure,
    PaymentActions.createCompensationPaymentFailure,
    (state, { error }) => ({
      ...state,
      loading: false,
      error
    })
  )
);
