import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RouterModule } from '@angular/router';
import { UserManagementComponent } from './user-management.component';
import * as AdminActions from '../../state/admin.actions';
import { selectAllAdminUsers, selectAdminLoading } from '../../state/admin.selectors';

describe('UserManagementComponent', () => {
  let component: UserManagementComponent;
  let fixture: ComponentFixture<UserManagementComponent>;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserManagementComponent, RouterModule.forRoot([])],
      providers: [
        provideMockStore({
          selectors: [
            { selector: selectAllAdminUsers, value: [] },
            { selector: selectAdminLoading, value: false }
          ]
        })
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(UserManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch loadAllUsers on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(AdminActions.loadAllUsers());
  });

  it('should dispatch updateUserStatus when onToggleStatus is called', () => {
    spyOn(store, 'dispatch');
    component.onToggleStatus('101', 'ACTIVE');
    expect(store.dispatch).toHaveBeenCalledWith(
      AdminActions.updateUserStatus({
        userId: 101,
        request: {
          status: 'SUSPENDED',
          reason: 'Administrative role moderation'
        }
      })
    );
  });
});
