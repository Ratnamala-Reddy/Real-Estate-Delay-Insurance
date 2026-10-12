import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RouterModule } from '@angular/router';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { ClaimListComponent } from './claim-list.component';
import * as ClaimActions from '../../state/claim.actions';
import { selectAllClaims, selectClaimLoading } from '../../state/claim.selectors';
import { selectUserRole } from '../../../auth/state/auth.selectors';
import { PaymentService } from '../../../payments/services/payment.service';

describe('ClaimListComponent', () => {
  let component: ClaimListComponent;
  let fixture: ComponentFixture<ClaimListComponent>;
  let store: MockStore;
  let paymentServiceSpy: jasmine.SpyObj<PaymentService>;

  beforeEach(async () => {
    paymentServiceSpy = jasmine.createSpyObj('PaymentService', ['getAllCompensationPayments']);
    paymentServiceSpy.getAllCompensationPayments.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [ClaimListComponent, RouterModule.forRoot([]), HttpClientTestingModule],
      providers: [
        provideMockStore({
          selectors: [
            { selector: selectAllClaims, value: [] },
            { selector: selectClaimLoading, value: false },
            { selector: selectUserRole, value: 'BUYER' }
          ]
        }),
        { provide: PaymentService, useValue: paymentServiceSpy }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(ClaimListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch loadClaims on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(ClaimActions.loadClaims());
  });

  it('should identify user as buyer', (done) => {
    component.isBuyer$.subscribe((isBuyer) => {
      expect(isBuyer).toBeTrue();
      done();
    });
  });
});
