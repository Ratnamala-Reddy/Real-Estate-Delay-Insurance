import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RoleGuard } from './role.guard';
import { selectUserRole } from '../../features/auth/state/auth.selectors';
import { NotificationService } from '../services/notification.service';

describe('RoleGuard', () => {
  let guard: RoleGuard;
  let store: MockStore;
  let routerSpy = { createUrlTree: jasmine.createSpy('createUrlTree') };
  let notificationSpy = { error: jasmine.createSpy('error') };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RoleGuard,
        provideMockStore(),
        { provide: Router, useValue: routerSpy },
        { provide: NotificationService, useValue: notificationSpy }
      ]
    });
    guard = TestBed.inject(RoleGuard);
    store = TestBed.inject(MockStore);
  });

  it('should allow access if role matches', (done) => {
    store.overrideSelector(selectUserRole, 'ADMIN');
    guard.canActivate({ data: { roles: ['ADMIN'] } } as any, { url: '/admin' } as any).subscribe(result => {
      expect(result).toBeTrue();
      done();
    });
  });

  it('should deny access and redirect if role does not match', (done) => {
    store.overrideSelector(selectUserRole, 'BUYER');
    guard.canActivate({ data: { roles: ['ADMIN'] } } as any, { url: '/admin' } as any).subscribe(() => {
      expect(notificationSpy.error).toHaveBeenCalled();
      expect(routerSpy.createUrlTree).toHaveBeenCalledWith(['/home']);
      done();
    });
  });
});
