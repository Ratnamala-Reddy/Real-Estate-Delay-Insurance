import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RouterModule } from '@angular/router';
import { of } from 'rxjs';
import { VerificationQueueComponent } from './verification-queue.component';
import * as VerificationActions from '../../state/verification.actions';
import * as ApplicationActions from '../../../applications/state/application.actions';
import { selectVerificationQueue, selectVerificationLoading } from '../../state/verification.selectors';
import { selectAllApplications, selectApplicationLoading } from '../../../applications/state/application.selectors';
import { AdminService } from '../../../admin/services/admin.service';
import { NotificationService } from '../../../../core/services/notification.service';

describe('VerificationQueueComponent', () => {
  let component: VerificationQueueComponent;
  let fixture: ComponentFixture<VerificationQueueComponent>;
  let store: MockStore;
  let adminServiceSpy: jasmine.SpyObj<AdminService>;
  let notificationServiceSpy: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    adminServiceSpy = jasmine.createSpyObj('AdminService', ['getAllUsers', 'updateUserStatus']);
    adminServiceSpy.getAllUsers.and.returnValue(of([]));
    notificationServiceSpy = jasmine.createSpyObj('NotificationService', ['success', 'error', 'info', 'warning']);

    await TestBed.configureTestingModule({
      imports: [VerificationQueueComponent, RouterModule.forRoot([])],
      providers: [
        provideMockStore({
          selectors: [
            { selector: selectVerificationQueue, value: [] },
            { selector: selectVerificationLoading, value: false },
            { selector: selectAllApplications, value: [] },
            { selector: selectApplicationLoading, value: false }
          ]
        }),
        { provide: AdminService, useValue: adminServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(VerificationQueueComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch loadVerificationQueue and loadAllApplications on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(VerificationActions.loadVerificationQueue());
    expect(store.dispatch).toHaveBeenCalledWith(ApplicationActions.loadAllApplications());
  });

  it('should switch filter tabs', () => {
    component.setTab('PENDING');
    expect(component.activeTab).toBe('PENDING');

    component.setTab('VERIFIED');
    expect(component.activeTab).toBe('VERIFIED');
  });
});
