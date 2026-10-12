import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectIsAuthenticated, selectUserRole, selectCurrentUser } from '../../../features/auth/state/auth.selectors';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  isAuthenticated$: Observable<boolean>;
  userRole$: Observable<string | null>;
  currentUser$: Observable<any>;

  constructor(private store: Store) {
    this.isAuthenticated$ = this.store.select(selectIsAuthenticated);
    this.userRole$ = this.store.select(selectUserRole);
    this.currentUser$ = this.store.select(selectCurrentUser);
  }
}
