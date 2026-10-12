import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { of } from 'rxjs';
import { ClaimAdjudicationComponent } from './claim-adjudication.component';
import * as ClaimActions from '../../state/claim.actions';
import { PaymentService } from '../../../payments/services/payment.service';
import { NotificationService } from '../../../../core/services/notification.service';

describe('ClaimAdjudicationComponent', () => {
  let component: ClaimAdjudicationComponent;
  let fixture: ComponentFixture<ClaimAdjudicationComponent>;
  let store: MockStore;
  let paymentServiceSpy: jasmine.SpyObj<PaymentService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    paymentServiceSpy = jasmine.createSpyObj('PaymentService', ['processCompensationWebhook']);
    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'error', 'info', 'warning']);

    await TestBed.configureTestingModule({
      imports: [ClaimAdjudicationComponent, RouterModule.forRoot([])],
      providers: [
        provideMockStore(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParams: { claimId: 'CLM-800' } },
            queryParams: of({ claimId: 'CLM-800' })
          }
        },
        { provide: PaymentService, useValue: paymentServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(ClaimAdjudicationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should dispatch loadClaimById on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(ClaimActions.loadClaimById({ claimId: 'CLM-800' }));
  });
});
