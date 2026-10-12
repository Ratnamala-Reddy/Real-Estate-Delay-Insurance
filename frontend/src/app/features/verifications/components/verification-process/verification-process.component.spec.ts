import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { of } from 'rxjs';
import { VerificationProcessComponent } from './verification-process.component';
import * as VerificationActions from '../../state/verification.actions';
import { ApplicationService } from '../../../applications/services/application.service';
import { AdminService } from '../../../admin/services/admin.service';
import { NotificationService } from '../../../../core/services/notification.service';

describe('VerificationProcessComponent', () => {
  let component: VerificationProcessComponent;
  let fixture: ComponentFixture<VerificationProcessComponent>;
  let store: MockStore;
  let applicationServiceSpy: jasmine.SpyObj<ApplicationService>;
  let adminServiceSpy: jasmine.SpyObj<AdminService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    applicationServiceSpy = jasmine.createSpyObj('ApplicationService', ['getApplicationById']);
    applicationServiceSpy.getApplicationById.and.returnValue(of({ applicationId: 'APP-300', userId: 1 } as any));

    adminServiceSpy = jasmine.createSpyObj('AdminService', ['getAllUsers', 'updateUserStatus']);
    adminServiceSpy.getAllUsers.and.returnValue(of([]));

    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'error', 'info', 'warning']);

    await TestBed.configureTestingModule({
      imports: [VerificationProcessComponent, ReactiveFormsModule, RouterModule.forRoot([])],
      providers: [
        provideMockStore(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'APP-300' } } }
        },
        { provide: ApplicationService, useValue: applicationServiceSpy },
        { provide: AdminService, useValue: adminServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(VerificationProcessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should dispatch loadVerificationResult on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(VerificationActions.loadVerificationResult({ applicationId: 'APP-300' }));
  });
});
