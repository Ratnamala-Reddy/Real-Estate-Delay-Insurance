import { createAction, props } from '@ngrx/store';
import {
  Application,
  CreateApplicationRequest,
  ApplicationDocument,
  UploadDocumentRequest,
  ApplicationStatusUpdateRequest
} from './application.models';

export const loadMyApplications = createAction('[Application] Load My Applications');
export const loadMyApplicationsSuccess = createAction(
  '[Application] Load My Applications Success',
  props<{ applications: Application[] }>()
);
export const loadMyApplicationsFailure = createAction(
  '[Application] Load My Applications Failure',
  props<{ error: string }>()
);

export const loadAllApplications = createAction('[Application] Load All Applications');
export const loadAllApplicationsSuccess = createAction(
  '[Application] Load All Applications Success',
  props<{ applications: Application[] }>()
);
export const loadAllApplicationsFailure = createAction(
  '[Application] Load All Applications Failure',
  props<{ error: string }>()
);

export const loadApplicationById = createAction(
  '[Application] Load Application By ID',
  props<{ applicationId: string }>()
);
export const loadApplicationByIdSuccess = createAction(
  '[Application] Load Application By ID Success',
  props<{ application: Application }>()
);
export const loadApplicationByIdFailure = createAction(
  '[Application] Load Application By ID Failure',
  props<{ error: string }>()
);

export const createApplication = createAction(
  '[Application] Create Application',
  props<{ request: CreateApplicationRequest }>()
);
export const createApplicationSuccess = createAction(
  '[Application] Create Application Success',
  props<{ application: Application }>()
);
export const createApplicationFailure = createAction(
  '[Application] Create Application Failure',
  props<{ error: string }>()
);

export const submitApplication = createAction(
  '[Application] Submit Application',
  props<{ applicationId: string }>()
);
export const submitApplicationSuccess = createAction(
  '[Application] Submit Application Success',
  props<{ application: Application }>()
);
export const submitApplicationFailure = createAction(
  '[Application] Submit Application Failure',
  props<{ error: string }>()
);

export const withdrawApplication = createAction(
  '[Application] Withdraw Application',
  props<{ applicationId: string }>()
);
export const withdrawApplicationSuccess = createAction(
  '[Application] Withdraw Application Success',
  props<{ application: Application }>()
);
export const withdrawApplicationFailure = createAction(
  '[Application] Withdraw Application Failure',
  props<{ error: string }>()
);

export const loadDocuments = createAction(
  '[Application] Load Documents',
  props<{ applicationId: string }>()
);
export const loadDocumentsSuccess = createAction(
  '[Application] Load Documents Success',
  props<{ documents: ApplicationDocument[] }>()
);
export const loadDocumentsFailure = createAction(
  '[Application] Load Documents Failure',
  props<{ error: string }>()
);

export const uploadDocument = createAction(
  '[Application] Upload Document',
  props<{ applicationId: string; request: UploadDocumentRequest }>()
);
export const uploadDocumentSuccess = createAction(
  '[Application] Upload Document Success',
  props<{ document: ApplicationDocument }>()
);
export const uploadDocumentFailure = createAction(
  '[Application] Upload Document Failure',
  props<{ error: string }>()
);
