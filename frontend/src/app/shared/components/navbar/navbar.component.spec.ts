import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from './navbar.component';
import * as AuthActions from '../../../features/auth/state/auth.actions';
import { selectIsAuthenticated, selectUserRole, selectCurrentUser } from '../../../features/auth/state/auth.selectors';

describe('NavbarComponent', () => {
  let component: NavbarComponent;
  let fixture: ComponentFixture<NavbarComponent>;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NavbarComponent, RouterModule.forRoot([])],
      providers: [
        provideMockStore({
          selectors: [
            { selector: selectIsAuthenticated, value: true },
            { selector: selectUserRole, value: 'BUYER' },
            { selector: selectCurrentUser, value: { firstName: 'Laxmi', lastName: 'Reddy', role: 'BUYER' } }
          ]
        })
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(NavbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create navbar', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle mobile menu', () => {
    expect(component.isMobileMenuOpen).toBeFalse();
    component.toggleMobileMenu();
    expect(component.isMobileMenuOpen).toBeTrue();
    component.closeMobileMenu();
    expect(component.isMobileMenuOpen).toBeFalse();
  });

  it('should dispatch logout action', () => {
    spyOn(store, 'dispatch');
    component.logout();
    expect(store.dispatch).toHaveBeenCalledWith(AuthActions.logout());
  });
});
