import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';
import { RoleGuard } from './core/guards/role.guard';

import { HomeComponent } from './shared/components/home/home.component';
import { NotFoundComponent } from './shared/components/not-found/not-found.component';

import { LoginComponent } from './features/auth/components/login/login.component';
import { RegisterComponent } from './features/auth/components/register/register.component';
import { ProfileComponent } from './features/auth/components/profile/profile.component';

import { ApplicationListComponent } from './features/applications/components/application-list/application-list.component';
import { ApplicationCreateComponent } from './features/applications/components/application-create/application-create.component';
import { ApplicationDetailComponent } from './features/applications/components/application-detail/application-detail.component';

import { VerificationQueueComponent } from './features/verifications/components/verification-queue/verification-queue.component';
import { VerificationProcessComponent } from './features/verifications/components/verification-process/verification-process.component';

import { RiskCalculatorComponent } from './features/risk/components/risk-calculator/risk-calculator.component';
import { RiskDetailComponent } from './features/risk/components/risk-detail/risk-detail.component';

import { QuoteSelectionComponent } from './features/quotes-policies/components/quote-selection/quote-selection.component';
import { PolicyListComponent } from './features/quotes-policies/components/policy-list/policy-list.component';
import { PolicyDetailComponent } from './features/quotes-policies/components/policy-detail/policy-detail.component';

import { PremiumCheckoutComponent } from './features/payments/components/premium-checkout/premium-checkout.component';
import { PaymentHistoryComponent } from './features/payments/components/payment-history/payment-history.component';

import { ClaimCreateComponent } from './features/claims/components/claim-create/claim-create.component';
import { ClaimListComponent } from './features/claims/components/claim-list/claim-list.component';
import { ClaimAdjudicationComponent } from './features/claims/components/claim-adjudication/claim-adjudication.component';

import { AdminDashboardComponent } from './features/admin/components/admin-dashboard/admin-dashboard.component';
import { UserManagementComponent } from './features/admin/components/user-management/user-management.component';

export const routes: Routes = [
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },

  // Auth Routes
  { path: 'auth/login', component: LoginComponent },
  { path: 'auth/register', component: RegisterComponent },
  {
    path: 'auth/profile',
    component: ProfileComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER', 'ADMIN'] }
  },

  // Applications Routes
  {
    path: 'applications',
    component: ApplicationListComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER', 'ADMIN'] }
  },
  {
    path: 'applications/new',
    component: ApplicationCreateComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER'] }
  },
  {
    path: 'applications/:id',
    component: ApplicationDetailComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER', 'ADMIN'] }
  },

  // Verifications Routes (Admin Underwriting)
  {
    path: 'verifications',
    component: VerificationQueueComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'verifications/process/:id',
    component: VerificationProcessComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },

  // Risk Assessment Routes
  {
    path: 'risk/calculator/:id',
    component: RiskCalculatorComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'risk/detail/:id',
    component: RiskDetailComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER', 'ADMIN'] }
  },

  // Quotes & Policies Routes
  {
    path: 'quotes/applications/:id',
    component: QuoteSelectionComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER', 'ADMIN'] }
  },
  {
    path: 'policies',
    component: PolicyListComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER', 'ADMIN'] }
  },
  {
    path: 'policies/:id',
    component: PolicyDetailComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER', 'ADMIN'] }
  },

  // Payments Routes
  {
    path: 'payments/checkout/:id',
    component: PremiumCheckoutComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER'] }
  },
  {
    path: 'payments/history',
    component: PaymentHistoryComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER', 'ADMIN'] }
  },

  // Claims Routes
  {
    path: 'claims',
    component: ClaimListComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER', 'ADMIN'] }
  },
  {
    path: 'claims/new',
    component: ClaimCreateComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['BUYER'] }
  },
  {
    path: 'claims/adjudication',
    component: ClaimAdjudicationComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },

  // Admin Routes
  { path: 'admin', redirectTo: 'admin/dashboard', pathMatch: 'full' },
  { path: 'dashboard', redirectTo: 'admin/dashboard', pathMatch: 'full' },
  {
    path: 'admin/dashboard',
    component: AdminDashboardComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  {
    path: 'admin/users',
    component: UserManagementComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }
  },
  { path: 'admin/policies', redirectTo: 'policies', pathMatch: 'full' },
  { path: 'allpolicies', redirectTo: 'policies', pathMatch: 'full' },
  { path: 'admin/verifications', redirectTo: 'verifications', pathMatch: 'full' },
  { path: 'verifications/queue', redirectTo: 'verifications', pathMatch: 'full' },
  { path: 'admin/claims', redirectTo: 'claims/adjudication', pathMatch: 'full' },

  // Wildcard 404
  { path: '**', component: NotFoundComponent }
];
