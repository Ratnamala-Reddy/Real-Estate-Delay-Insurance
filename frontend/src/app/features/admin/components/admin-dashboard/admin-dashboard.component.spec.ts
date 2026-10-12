import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RouterModule } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { AdminDashboardComponent } from './admin-dashboard.component';
import * as AdminActions from '../../state/admin.actions';
import * as ApplicationActions from '../../../applications/state/application.actions';
import * as PolicyActions from '../../../quotes-policies/state/policy.actions';
import * as ClaimActions from '../../../claims/state/claim.actions';
import { selectAllAdminUsers } from '../../state/admin.selectors';
import { selectAllApplications } from '../../../applications/state/application.selectors';
import { selectAllPolicies } from '../../../quotes-policies/state/policy.selectors';
import { selectAllClaims } from '../../../claims/state/claim.selectors';
import { selectCurrentUser } from '../../../auth/state/auth.selectors';
import { PaymentService } from '../../../payments/services/payment.service';
import { NotificationService } from '../../../../core/services/notification.service';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let store: MockStore;
  let paymentServiceSpy: jasmine.SpyObj<PaymentService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    paymentServiceSpy = jasmine.createSpyObj('PaymentService', [
      'getAllPremiumPayments',
      'getAllCompensationPayments',
      'processPremiumWebhook',
      'processCompensationWebhook'
    ]);
    paymentServiceSpy.getAllPremiumPayments.and.returnValue(of([]));
    paymentServiceSpy.getAllCompensationPayments.and.returnValue(of([]));

    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'warning', 'error', 'info']);

    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent, RouterModule.forRoot([]), HttpClientTestingModule],
      providers: [
        provideMockStore({
          selectors: [
            { selector: selectCurrentUser, value: { firstName: 'Admin', lastName: 'User', role: 'ADMIN' } },
            { selector: selectAllAdminUsers, value: [] },
            { selector: selectAllApplications, value: [] },
            { selector: selectAllPolicies, value: [] },
            { selector: selectAllClaims, value: [] }
          ]
        }),
        { provide: PaymentService, useValue: paymentServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch all load actions on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(AdminActions.loadAllUsers());
    expect(store.dispatch).toHaveBeenCalledWith(ApplicationActions.loadAllApplications());
    expect(store.dispatch).toHaveBeenCalledWith(PolicyActions.loadPolicies());
    expect(store.dispatch).toHaveBeenCalledWith(ClaimActions.loadClaims());
  });

  it('should load all payments on init', () => {
    expect(paymentServiceSpy.getAllPremiumPayments).toHaveBeenCalled();
    expect(paymentServiceSpy.getAllCompensationPayments).toHaveBeenCalled();
  });
});
