import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { of } from 'rxjs';
import { RiskCalculatorComponent } from './risk-calculator.component';
import * as RiskActions from '../../state/risk.actions';
import { ApplicationService } from '../../../applications/services/application.service';
import { NotificationService } from '../../../../core/services/notification.service';

describe('RiskCalculatorComponent', () => {
  let component: RiskCalculatorComponent;
  let fixture: ComponentFixture<RiskCalculatorComponent>;
  let store: MockStore;
  let applicationServiceSpy: jasmine.SpyObj<ApplicationService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    applicationServiceSpy = jasmine.createSpyObj('ApplicationService', ['getApplicationById', 'getDocuments']);
    applicationServiceSpy.getApplicationById.and.returnValue(of({ applicationId: 'APP-400' } as any));
    applicationServiceSpy.getDocuments.and.returnValue(of([]));

    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'error', 'info', 'warning']);

    await TestBed.configureTestingModule({
      imports: [RiskCalculatorComponent, ReactiveFormsModule, RouterModule.forRoot([])],
      providers: [
        provideMockStore(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'APP-400' } } }
        },
        { provide: ApplicationService, useValue: applicationServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(RiskCalculatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should dispatch loadRiskAssessment on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(RiskActions.loadRiskAssessment({ applicationId: 'APP-400' }));
  });
});
