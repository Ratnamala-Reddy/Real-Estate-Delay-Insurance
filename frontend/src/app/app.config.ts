import { ApplicationConfig, provideZoneChangeDetection, isDevMode } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';

import { routes } from './app.routes';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';

import { authReducer } from './features/auth/state/auth.reducer';
import { AuthEffects } from './features/auth/state/auth.effects';

import { applicationReducer } from './features/applications/state/application.reducer';
import { ApplicationEffects } from './features/applications/state/application.effects';

import { verificationReducer } from './features/verifications/state/verification.reducer';
import { VerificationEffects } from './features/verifications/state/verification.effects';

import { riskReducer } from './features/risk/state/risk.reducer';
import { RiskEffects } from './features/risk/state/risk.effects';

import { policyReducer } from './features/quotes-policies/state/policy.reducer';
import { PolicyEffects } from './features/quotes-policies/state/policy.effects';

import { paymentReducer } from './features/payments/state/payment.reducer';
import { PaymentEffects } from './features/payments/state/payment.effects';

import { claimReducer } from './features/claims/state/claim.reducer';
import { ClaimEffects } from './features/claims/state/claim.effects';

import { adminReducer } from './features/admin/state/admin.reducer';
import { AdminEffects } from './features/admin/state/admin.effects';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptorsFromDi()),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    },
    provideStore({
      auth: authReducer,
      applications: applicationReducer,
      verifications: verificationReducer,
      risk: riskReducer,
      policy: policyReducer,
      payment: paymentReducer,
      claims: claimReducer,
      admin: adminReducer
    }),
    provideEffects([
      AuthEffects,
      ApplicationEffects,
      VerificationEffects,
      RiskEffects,
      PolicyEffects,
      PaymentEffects,
      ClaimEffects,
      AdminEffects
    ]),
    provideStoreDevtools({ maxAge: 25, logOnly: !isDevMode() })
  ]
};
