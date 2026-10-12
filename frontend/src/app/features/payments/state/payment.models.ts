export type PremiumPaymentStatus = 'INITIATED' | 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED' | 'REFUNDED';
export type CompensationPaymentStatus = 'INITIATED' | 'PENDING' | 'PAID' | 'FAILED';

export interface PremiumPaymentRequest {
  quoteId: string;
  paymentProvider: string;
}

export interface PremiumPayment {
  paymentId: string;
  quoteId: string;
  applicationId: string;
  premiumAmount: number;
  paymentProvider: string;
  transactionReference: string;
  status: PremiumPaymentStatus;
  createdAt: string;
}

export interface CompensationPaymentRequest {
  claimId: string;
  policyId: string;
  payoutAmount: number;
}

export interface CompensationPayment {
  paymentId: string;
  claimId: string;
  policyId: string;
  payoutAmount: number;
  transactionReference: string;
  status: CompensationPaymentStatus;
  createdAt: string;
}

export interface PaymentState {
  currentPremiumPayment: PremiumPayment | null;
  compensationPayments: CompensationPayment[];
  loading: boolean;
  error: string | null;
}
