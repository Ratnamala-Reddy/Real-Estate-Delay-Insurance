import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { Store } from '@ngrx/store';
import { map, take } from 'rxjs/operators';
import { selectIsAuthenticated } from '../../features/auth/state/auth.selectors';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  constructor(private store: Store, private router: Router) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> {
    return this.store.select(selectIsAuthenticated).pipe(
      take(1),
      map(isAuthenticated => {
        if (isAuthenticated) {
          return true;
        }
        return this.router.createUrlTree(['/auth/login'], {
          queryParams: { returnUrl: state.url }
        });
      })
    );
  }
}
