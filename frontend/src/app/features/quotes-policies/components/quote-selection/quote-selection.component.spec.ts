import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { of } from 'rxjs';
import { QuoteSelectionComponent } from './quote-selection.component';
import * as PolicyActions from '../../state/policy.actions';
import { PaymentService } from '../../../payments/services/payment.service';

describe('QuoteSelectionComponent', () => {
  let component: QuoteSelectionComponent;
  let fixture: ComponentFixture<QuoteSelectionComponent>;
  let store: MockStore;
  let mockPaymentService: jasmine.SpyObj<PaymentService>;

  beforeEach(async () => {
    mockPaymentService = jasmine.createSpyObj('PaymentService', ['getAllPremiumPayments']);
    mockPaymentService.getAllPremiumPayments.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [
        QuoteSelectionComponent,
        ReactiveFormsModule,
        HttpClientTestingModule,
        RouterModule.forRoot([])
      ],
      providers: [
        provideMockStore(),
        { provide: PaymentService, useValue: mockPaymentService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'APP-600' } } }
        }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(QuoteSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should dispatch loadQuotes on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(PolicyActions.loadQuotes({ applicationId: 'APP-600' }));
  });

  it('should identify whether quote is paid', () => {
    expect(component.isQuotePaid('QTE-123')).toBeFalse();
    component.paidQuoteIds.add('QTE-123');
    expect(component.isQuotePaid('QTE-123')).toBeTrue();
  });
});
