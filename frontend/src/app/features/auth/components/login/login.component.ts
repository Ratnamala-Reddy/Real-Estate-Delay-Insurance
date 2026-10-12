import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import * as AuthActions from '../../state/auth.actions';
import { selectAuthError, selectAuthLoading } from '../../state/auth.selectors';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  loading$: Observable<boolean>;
  error$: Observable<string | null>;
  returnUrl?: string;

  constructor(
    private fb: FormBuilder,
    private store: Store,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
    this.loading$ = this.store.select(selectAuthLoading);
    this.error$ = this.store.select(selectAuthError);
  }

  ngOnInit(): void {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'];
    // Ensure clean login state without lingering previous sessions
    if (localStorage.getItem('access_token') || localStorage.getItem('user_role')) {
      localStorage.removeItem('access_token');
      localStorage.removeItem('user_role');
      this.store.dispatch(AuthActions.logoutSuccess());
    }
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.store.dispatch(
        AuthActions.login({
          request: this.loginForm.value,
          returnUrl: this.returnUrl
        })
      );
    } else {
      this.loginForm.markAllAsTouched();
    }
  }
}
