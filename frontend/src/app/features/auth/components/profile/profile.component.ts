import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectCurrentUser, selectAuthLoading } from '../../state/auth.selectors';
import * as AuthActions from '../../state/auth.actions';
import { UserProfileResponse } from '../../state/auth.models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {
  profileForm: FormGroup;
  currentUser$: Observable<UserProfileResponse | null>;
  loading$: Observable<boolean>;

  constructor(private fb: FormBuilder, private store: Store) {
    this.profileForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      mobileNumber: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]]
    });
    this.currentUser$ = this.store.select(selectCurrentUser);
    this.loading$ = this.store.select(selectAuthLoading);
  }

  ngOnInit(): void {
    this.store.dispatch(AuthActions.loadProfile());
    this.currentUser$.subscribe((user) => {
      if (user) {
        this.profileForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          mobileNumber: user.mobileNumber
        });
      }
    });
  }

  onSubmit(): void {
    if (this.profileForm.valid) {
      this.store.dispatch(AuthActions.updateProfile({ request: this.profileForm.value }));
    }
  }
}
