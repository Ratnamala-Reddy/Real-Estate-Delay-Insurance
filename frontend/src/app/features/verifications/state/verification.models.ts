export type VerificationStatus =
  | 'PENDING'
  | 'PROPERTY_VERIFIED'
  | 'ADDITIONAL_INFORMATION_REQUIRED'
  | 'VERIFIED'
  | 'REJECTED';

export interface VerificationRecord {
  verificationId: string;
  applicationId: string;
  verificationStatus: VerificationStatus;
  remarks: string;
  verifiedBy: string;
  verifiedAt: string;
}

export interface ApplicationQueueItem {
  applicationId: string;
  userId: number;
  builderName: string;
  projectName: string;
  unitNumber: string;
  propertyValue: number;
  purchaseDate: string;
  promisedPossessionDate: string;
  status: string;
}

export interface PropertyVerificationRequest {
  remarks: string;
  verifiedBy: string;
}

export interface AdditionalInfoRequest {
  remarks: string;
  requestedBy: string;
}

export interface ApprovalRequest {
  remarks: string;
  approvedBy: string;
}

export interface RejectVerificationRequest {
  remarks: string;
  rejectedBy: string;
}

export interface VerificationState {
  queue: ApplicationQueueItem[];
  currentRecord: VerificationRecord | null;
  loading: boolean;
  error: string | null;
}
