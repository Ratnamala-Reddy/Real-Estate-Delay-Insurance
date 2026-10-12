import { PaymentState, PremiumPayment } from './payment.models';
import {
  selectCurrentPremiumPayment,
  selectPaymentLoading,
  selectPaymentError
} from './payment.selectors';

describe('Payment Selectors', () => {
  const mockPayment: PremiumPayment = {
    paymentId: 'PAY-001',
    applicationId: 'APP-001',
    quoteId: 'QUO-001',
    premiumAmount: 12500,
    paymentProvider: 'ESCROW_SIMULATOR',
    transactionReference: 'TXN-001',
    status: 'SUCCESS',
    createdAt: '2026-10-12T10:00:00'
  };

  const state: PaymentState = {
    currentPremiumPayment: mockPayment,
    compensationPayments: [],
    loading: false,
    error: null
  };

  it('selectCurrentPremiumPayment should return current payment', () => {
    expect(selectCurrentPremiumPayment.projector(state)).toEqual(mockPayment);
  });

  it('selectPaymentLoading should return loading status', () => {
    expect(selectPaymentLoading.projector(state)).toBeFalse();
  });

  it('selectPaymentError should return error', () => {
    expect(selectPaymentError.projector(state)).toBeNull();
  });
});

