import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { of } from 'rxjs';
import { PremiumCheckoutComponent } from './premium-checkout.component';
import { PaymentService } from '../../services/payment.service';
import { PolicyService } from '../../../quotes-policies/services/policy.service';

describe('PremiumCheckoutComponent', () => {
  let component: PremiumCheckoutComponent;
  let fixture: ComponentFixture<PremiumCheckoutComponent>;
  let store: MockStore;
  let mockPaymentService: jasmine.SpyObj<PaymentService>;
  let mockPolicyService: jasmine.SpyObj<PolicyService>;

  beforeEach(async () => {
    mockPaymentService = jasmine.createSpyObj('PaymentService', ['getAllPremiumPayments']);
    mockPaymentService.getAllPremiumPayments.and.returnValue(of([]));

    mockPolicyService = jasmine.createSpyObj('PolicyService', ['getQuoteById']);
    mockPolicyService.getQuoteById.and.returnValue(of({
      quoteId: 'Q-999',
      applicationId: 'APP-101',
      planName: 'STANDARD',
      premiumAmount: 18500,
      monthlyCompensation: 25000,
      maximumDelayMonths: 12,
      maximumPayout: 300000,
      gracePeriodDays: 30,
      expiryDate: '2026-12-31',
      status: 'GENERATED',
      createdAt: '2026-01-01'
    }));

    await TestBed.configureTestingModule({
      imports: [
        PremiumCheckoutComponent,
        ReactiveFormsModule,
        HttpClientTestingModule,
        RouterModule.forRoot([])
      ],
      providers: [
        provideMockStore(),
        { provide: PaymentService, useValue: mockPaymentService },
        { provide: PolicyService, useValue: mockPolicyService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'Q-999' } } }
        }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(PremiumCheckoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with quoteId', () => {
    expect(component.quoteId).toBe('Q-999');
    expect(component.isPaymentDoneForMonth).toBeFalse();
  });

  it('should disable submit when payment is already done for month', () => {
    component.isPaymentDoneForMonth = true;
    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(submitBtn.disabled).toBeTrue();
    expect(submitBtn.textContent).toContain('Payment Done for This Month');
  });
});
