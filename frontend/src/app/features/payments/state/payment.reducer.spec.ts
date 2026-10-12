import { paymentReducer, initialPaymentState } from './payment.reducer';
import * as PaymentActions from './payment.actions';
import { PremiumPayment } from './payment.models';

describe('Payment Reducer', () => {
  const mockPayment: PremiumPayment = {
    paymentId: 'PAY-001',
    applicationId: 'APP-001',
    quoteId: 'QUO-001',
    premiumAmount: 18500,
    paymentProvider: 'ESCROW_SIMULATOR',
    status: 'SUCCESS',
    transactionReference: 'TXN-12345',
    createdAt: '2026-10-12T10:00:00'
  };

  it('should set loading to true on createPremiumPayment', () => {
    const state = paymentReducer(
      initialPaymentState,
      PaymentActions.createPremiumPayment({ request: { quoteId: 'QUO-001', paymentProvider: 'ESCROW_SIMULATOR' } })
    );
    expect(state.loading).toBeTrue();
    expect(state.error).toBeNull();
  });

  it('should store payment on createPremiumPaymentSuccess', () => {
    const state = paymentReducer(
      { ...initialPaymentState, loading: true },
      PaymentActions.createPremiumPaymentSuccess({ payment: mockPayment })
    );
    expect(state.loading).toBeFalse();
    expect(state.currentPremiumPayment).toEqual(mockPayment);
  });

  it('should set error on createPremiumPaymentFailure', () => {
    const state = paymentReducer(
      { ...initialPaymentState, loading: true },
      PaymentActions.createPremiumPaymentFailure({ error: 'Gateway timeout' })
    );
    expect(state.loading).toBeFalse();
    expect(state.error).toBe('Gateway timeout');
  });
});

