export type ApplicationStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'VERIFICATION_IN_PROGRESS'
  | 'ADDITIONAL_INFORMATION_REQUIRED'
  | 'VERIFIED'
  | 'REJECTED'
  | 'WITHDRAWN';

export interface CreateApplicationRequest {
  builderName: string;
  projectName: string;
  unitNumber: string;
  propertyValue: number;
  purchaseDate: string; // ISO date YYYY-MM-DD
  promisedPossessionDate: string; // ISO date YYYY-MM-DD
}

export interface Application {
  applicationId: string;
  userId: number;
  builderName: string;
  projectName: string;
  unitNumber: string;
  propertyValue: number;
  purchaseDate: string;
  promisedPossessionDate: string;
  status: ApplicationStatus;
}

export interface UploadDocumentRequest {
  documentType: string;
  fileName: string;
  filePath: string;
}

export interface ApplicationDocument {
  documentId: string;
  applicationId: string;
  documentType: string;
  fileName: string;
  filePath: string;
  reviewStatus: string;
}

export interface ApplicationStatusUpdateRequest {
  status: string;
}

export interface ApplicationState {
  applications: Application[];
  selectedApplication: Application | null;
  documents: ApplicationDocument[];
  loading: boolean;
  error: string | null;
}
