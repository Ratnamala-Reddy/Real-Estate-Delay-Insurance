import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { AuthGuard } from './auth.guard';
import { selectIsAuthenticated } from '../../features/auth/state/auth.selectors';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let store: MockStore;
  let routerSpy = { createUrlTree: jasmine.createSpy('createUrlTree') };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        provideMockStore(),
        { provide: Router, useValue: routerSpy }
      ]
    });
    guard = TestBed.inject(AuthGuard);
    store = TestBed.inject(MockStore);
  });

  it('should allow access if authenticated', (done) => {
    store.overrideSelector(selectIsAuthenticated, true);
    guard.canActivate({} as any, { url: '/dashboard' } as any).subscribe(result => {
      expect(result).toBeTrue();
      done();
    });
  });

  it('should redirect to login if not authenticated', (done) => {
    store.overrideSelector(selectIsAuthenticated, false);
    guard.canActivate({} as any, { url: '/dashboard' } as any).subscribe(() => {
      expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/auth/login'], { queryParams: { returnUrl: '/dashboard' } });
      done();
    });
  });
});
