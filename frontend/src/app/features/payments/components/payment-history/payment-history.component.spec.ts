import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore } from '@ngrx/store/testing';
import { of } from 'rxjs';
import { RouterModule } from '@angular/router';
import { PaymentHistoryComponent } from './payment-history.component';
import { PaymentService } from '../../services/payment.service';
import { NotificationService } from '../../../../core/services/notification.service';

describe('PaymentHistoryComponent', () => {
  let component: PaymentHistoryComponent;
  let fixture: ComponentFixture<PaymentHistoryComponent>;
  let paymentServiceSpy: jasmine.SpyObj<PaymentService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    paymentServiceSpy = jasmine.createSpyObj('PaymentService', [
      'getAllPremiumPayments',
      'getAllCompensationPayments',
      'processPremiumWebhook'
    ]);
    paymentServiceSpy.getAllPremiumPayments.and.returnValue(of([]));
    paymentServiceSpy.getAllCompensationPayments.and.returnValue(of([]));

    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'error', 'info', 'warning']);

    await TestBed.configureTestingModule({
      imports: [PaymentHistoryComponent, RouterModule.forRoot([])],
      providers: [
        provideMockStore(),
        { provide: PaymentService, useValue: paymentServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentHistoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create payment history component', () => {
    expect(component).toBeTruthy();
  });
});
