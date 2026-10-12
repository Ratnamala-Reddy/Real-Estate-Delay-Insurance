import { createFeatureSelector, createSelector } from '@ngrx/store';
import { PaymentState } from './payment.models';

export const selectPaymentState = createFeatureSelector<PaymentState>('payment');

export const selectCurrentPremiumPayment = createSelector(
  selectPaymentState,
  (state) => state?.currentPremiumPayment || null
);

export const selectPaymentLoading = createSelector(
  selectPaymentState,
  (state) => !!state?.loading
);

export const selectPaymentError = createSelector(
  selectPaymentState,
  (state) => state?.error || null
);
