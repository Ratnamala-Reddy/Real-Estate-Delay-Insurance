import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from './shared/components/navbar/navbar.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ToastComponent } from './shared/components/toast/toast.component';
import { Store } from '@ngrx/store';
import * as AuthActions from './features/auth/state/auth.actions';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent, FooterComponent, ToastComponent],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'PropProtect - Real Estate Project Delay Insurance Platform';

  constructor(private store: Store) {}

  ngOnInit(): void {
    const token = localStorage.getItem('access_token');
    const role = localStorage.getItem('user_role');
    if (token && role === 'BUYER') {
      this.store.dispatch(AuthActions.loadProfile());
    }
  }
}
