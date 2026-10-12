import { createAction, props } from '@ngrx/store';
import {
  ApplicationQueueItem,
  VerificationRecord,
  PropertyVerificationRequest,
  AdditionalInfoRequest,
  ApprovalRequest,
  RejectVerificationRequest
} from './verification.models';

export const loadVerificationQueue = createAction('[Verification] Load Queue');
export const loadVerificationQueueSuccess = createAction(
  '[Verification] Load Queue Success',
  props<{ queue: ApplicationQueueItem[] }>()
);
export const loadVerificationQueueFailure = createAction(
  '[Verification] Load Queue Failure',
  props<{ error: string }>()
);

export const loadVerificationResult = createAction(
  '[Verification] Load Result',
  props<{ applicationId: string }>()
);
export const loadVerificationResultSuccess = createAction(
  '[Verification] Load Result Success',
  props<{ record: VerificationRecord }>()
);
export const loadVerificationResultFailure = createAction(
  '[Verification] Load Result Failure',
  props<{ error: string }>()
);

export const verifyProperty = createAction(
  '[Verification] Verify Property',
  props<{ applicationId: string; request: PropertyVerificationRequest }>()
);
export const verifyPropertySuccess = createAction(
  '[Verification] Verify Property Success',
  props<{ record: VerificationRecord }>()
);
export const verifyPropertyFailure = createAction(
  '[Verification] Verify Property Failure',
  props<{ error: string }>()
);

export const requestAdditionalInfo = createAction(
  '[Verification] Request Additional Info',
  props<{ applicationId: string; request: AdditionalInfoRequest }>()
);
export const requestAdditionalInfoSuccess = createAction(
  '[Verification] Request Additional Info Success',
  props<{ record: VerificationRecord }>()
);
export const requestAdditionalInfoFailure = createAction(
  '[Verification] Request Additional Info Failure',
  props<{ error: string }>()
);

export const approveVerification = createAction(
  '[Verification] Approve Verification',
  props<{ applicationId: string; request: ApprovalRequest }>()
);
export const approveVerificationSuccess = createAction(
  '[Verification] Approve Verification Success',
  props<{ record: VerificationRecord }>()
);
export const approveVerificationFailure = createAction(
  '[Verification] Approve Verification Failure',
  props<{ error: string }>()
);

export const rejectVerification = createAction(
  '[Verification] Reject Verification',
  props<{ applicationId: string; request: RejectVerificationRequest }>()
);
export const rejectVerificationSuccess = createAction(
  '[Verification] Reject Verification Success',
  props<{ record: VerificationRecord }>()
);
export const rejectVerificationFailure = createAction(
  '[Verification] Reject Verification Failure',
  props<{ error: string }>()
);
