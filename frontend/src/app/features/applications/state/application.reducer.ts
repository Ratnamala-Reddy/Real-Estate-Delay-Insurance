import { createReducer, on } from '@ngrx/store';
import { ApplicationState } from './application.models';
import * as ApplicationActions from './application.actions';

export const initialApplicationState: ApplicationState = {
  applications: [],
  selectedApplication: null,
  documents: [],
  loading: false,
  error: null
};

export const applicationReducer = createReducer(
  initialApplicationState,
  on(
    ApplicationActions.loadMyApplications,
    ApplicationActions.loadAllApplications,
    ApplicationActions.loadApplicationById,
    ApplicationActions.createApplication,
    ApplicationActions.submitApplication,
    ApplicationActions.withdrawApplication,
    ApplicationActions.loadDocuments,
    ApplicationActions.uploadDocument,
    (state) => ({ ...state, loading: true, error: null })
  ),
  on(
    ApplicationActions.loadMyApplicationsSuccess,
    ApplicationActions.loadAllApplicationsSuccess,
    (state, { applications }) => ({
      ...state,
      loading: false,
      applications,
      error: null
    })
  ),
  on(
    ApplicationActions.loadApplicationByIdSuccess,
    ApplicationActions.createApplicationSuccess,
    ApplicationActions.submitApplicationSuccess,
    ApplicationActions.withdrawApplicationSuccess,
    (state, { application }) => ({
      ...state,
      loading: false,
      selectedApplication: application,
      applications: state.applications.some((a) => a.applicationId === application.applicationId)
        ? state.applications.map((a) => (a.applicationId === application.applicationId ? application : a))
        : [application, ...state.applications],
      error: null
    })
  ),
  on(ApplicationActions.loadDocumentsSuccess, (state, { documents }) => ({
    ...state,
    loading: false,
    documents,
    error: null
  })),
  on(ApplicationActions.uploadDocumentSuccess, (state, { document }) => ({
    ...state,
    loading: false,
    documents: [...state.documents, document],
    error: null
  })),
  on(
    ApplicationActions.loadMyApplicationsFailure,
    ApplicationActions.loadAllApplicationsFailure,
    ApplicationActions.loadApplicationByIdFailure,
    ApplicationActions.createApplicationFailure,
    ApplicationActions.submitApplicationFailure,
    ApplicationActions.withdrawApplicationFailure,
    ApplicationActions.loadDocumentsFailure,
    ApplicationActions.uploadDocumentFailure,
    (state, { error }) => ({
      ...state,
      loading: false,
      error
    })
  )
);
