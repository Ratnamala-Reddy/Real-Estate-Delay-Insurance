import { createAction, props } from '@ngrx/store';
import {
  PremiumPayment,
  PremiumPaymentRequest,
  CompensationPayment,
  CompensationPaymentRequest
} from './payment.models';

export const createPremiumPayment = createAction(
  '[Payment] Create Premium Payment',
  props<{ request: PremiumPaymentRequest }>()
);
export const createPremiumPaymentSuccess = createAction(
  '[Payment] Create Premium Payment Success',
  props<{ payment: PremiumPayment }>()
);
export const createPremiumPaymentFailure = createAction(
  '[Payment] Create Premium Payment Failure',
  props<{ error: string }>()
);

export const loadPremiumPayment = createAction(
  '[Payment] Load Premium Payment',
  props<{ paymentId: string }>()
);
export const loadPremiumPaymentSuccess = createAction(
  '[Payment] Load Premium Payment Success',
  props<{ payment: PremiumPayment }>()
);
export const loadPremiumPaymentFailure = createAction(
  '[Payment] Load Premium Payment Failure',
  props<{ error: string }>()
);

export const createCompensationPayment = createAction(
  '[Payment] Create Compensation Payment',
  props<{ request: CompensationPaymentRequest }>()
);
export const createCompensationPaymentSuccess = createAction(
  '[Payment] Create Compensation Payment Success',
  props<{ payment: CompensationPayment }>()
);
export const createCompensationPaymentFailure = createAction(
  '[Payment] Create Compensation Payment Failure',
  props<{ error: string }>()
);
