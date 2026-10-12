import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { Store } from '@ngrx/store';
import { map, take } from 'rxjs/operators';
import { selectUserRole, selectIsAuthenticated } from '../../features/auth/state/auth.selectors';
import { NotificationService } from '../services/notification.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  constructor(
    private store: Store,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> {
    const expectedRoles: string[] = (route.data['roles'] || []).map((r: string) => r.toUpperCase());

    return this.store.select(selectUserRole).pipe(
      take(1),
      map(userRole => {
        const activeRole = (userRole || (typeof localStorage !== 'undefined' ? localStorage.getItem('user_role') : '') || '').trim().toUpperCase();
        if (!activeRole) {
          return this.router.createUrlTree(['/auth/login']);
        }
        if (expectedRoles.length === 0 || expectedRoles.includes(activeRole)) {
          return true;
        }
        this.notificationService.error('Access Denied', 'You do not have permission to view this page');
        return this.router.createUrlTree(['/home']);
      })
    );
  }
}
