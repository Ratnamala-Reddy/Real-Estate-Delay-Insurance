import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable, filter, map, startWith } from 'rxjs';
import { selectIsAuthenticated, selectUserRole, selectCurrentUser } from '../../../features/auth/state/auth.selectors';
import * as AuthActions from '../../../features/auth/state/auth.actions';
import { HasRoleDirective } from '../../../core/directives/has-role.directive';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, HasRoleDirective],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  isAuthenticated$: Observable<boolean>;
  userRole$: Observable<string | null>;
  currentUser$: Observable<any>;
  isMobileMenuOpen = false;
  currentUrl$: Observable<string>;

  constructor(
    private store: Store,
    private router: Router
  ) {
    this.isAuthenticated$ = this.store.select(selectIsAuthenticated);
    this.userRole$ = this.store.select(selectUserRole);
    this.currentUser$ = this.store.select(selectCurrentUser);

    this.currentUrl$ = this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects || e.url),
      startWith(this.router.url)
    );
  }

  ngOnInit(): void {}

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  getBrandLink(role: string | null): string {
    if (role === 'ADMIN') return '/admin/dashboard';
    if (role === 'BUYER') return '/applications';
    return '/auth/login';
  }

  logout(): void {
    this.closeMobileMenu();
    this.store.dispatch(AuthActions.logout());
  }
}
