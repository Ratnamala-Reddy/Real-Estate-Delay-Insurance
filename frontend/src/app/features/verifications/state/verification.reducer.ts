import { createReducer, on } from '@ngrx/store';
import { VerificationState } from './verification.models';
import * as VerificationActions from './verification.actions';

export const initialVerificationState: VerificationState = {
  queue: [],
  currentRecord: null,
  loading: false,
  error: null
};

export const verificationReducer = createReducer(
  initialVerificationState,
  on(
    VerificationActions.loadVerificationQueue,
    VerificationActions.loadVerificationResult,
    VerificationActions.verifyProperty,
    VerificationActions.requestAdditionalInfo,
    VerificationActions.approveVerification,
    VerificationActions.rejectVerification,
    (state) => ({ ...state, loading: true, error: null })
  ),
  on(VerificationActions.loadVerificationQueueSuccess, (state, { queue }) => ({
    ...state,
    loading: false,
    queue,
    error: null
  })),
  on(
    VerificationActions.loadVerificationResultSuccess,
    VerificationActions.verifyPropertySuccess,
    VerificationActions.requestAdditionalInfoSuccess,
    VerificationActions.approveVerificationSuccess,
    VerificationActions.rejectVerificationSuccess,
    (state, { record }) => ({
      ...state,
      loading: false,
      currentRecord: record,
      error: null
    })
  ),
  on(
    VerificationActions.loadVerificationQueueFailure,
    VerificationActions.loadVerificationResultFailure,
    VerificationActions.verifyPropertyFailure,
    VerificationActions.requestAdditionalInfoFailure,
    VerificationActions.approveVerificationFailure,
    VerificationActions.rejectVerificationFailure,
    (state, { error }) => ({
      ...state,
      loading: false,
      error
    })
  )
);
