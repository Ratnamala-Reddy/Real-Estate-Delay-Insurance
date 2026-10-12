import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RouterModule } from '@angular/router';
import { PolicyListComponent } from './policy-list.component';
import * as PolicyActions from '../../state/policy.actions';
import { selectAllPolicies, selectPolicyLoading } from '../../state/policy.selectors';
import { selectUserRole } from '../../../auth/state/auth.selectors';

describe('PolicyListComponent', () => {
  let component: PolicyListComponent;
  let fixture: ComponentFixture<PolicyListComponent>;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PolicyListComponent, RouterModule.forRoot([])],
      providers: [
        provideMockStore({
          selectors: [
            { selector: selectAllPolicies, value: [] },
            { selector: selectPolicyLoading, value: false },
            { selector: selectUserRole, value: 'BUYER' }
          ]
        })
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(PolicyListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch loadPolicies on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(PolicyActions.loadPolicies());
  });

  it('should determine buyer role', (done) => {
    component.isBuyer$.subscribe((isBuyer) => {
      expect(isBuyer).toBeTrue();
      done();
    });
  });
});
