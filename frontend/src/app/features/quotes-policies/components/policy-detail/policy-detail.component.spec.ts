import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { of } from 'rxjs';
import { PolicyDetailComponent } from './policy-detail.component';
import * as PolicyActions from '../../state/policy.actions';
import { PolicyService } from '../../services/policy.service';
import { NotificationService } from '../../../../core/services/notification.service';

describe('PolicyDetailComponent', () => {
  let component: PolicyDetailComponent;
  let fixture: ComponentFixture<PolicyDetailComponent>;
  let store: MockStore;
  let policyServiceSpy: jasmine.SpyObj<PolicyService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    policyServiceSpy = jasmine.createSpyObj('PolicyService', ['endorsePolicy', 'cancelPolicy', 'renewPolicy']);
    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'error', 'info', 'warning']);

    await TestBed.configureTestingModule({
      imports: [PolicyDetailComponent, RouterModule.forRoot([])],
      providers: [
        provideMockStore(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'POL-700' } } }
        },
        { provide: PolicyService, useValue: policyServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(PolicyDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should dispatch loadPolicyById on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(PolicyActions.loadPolicyById({ policyId: 'POL-700' }));
  });
});
