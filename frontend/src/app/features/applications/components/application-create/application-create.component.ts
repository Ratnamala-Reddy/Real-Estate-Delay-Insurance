import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import * as ApplicationActions from '../../state/application.actions';
import { selectApplicationLoading } from '../../state/application.selectors';

@Component({
  selector: 'app-application-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './application-create.component.html',
  styleUrls: ['./application-create.component.css']
})
export class ApplicationCreateComponent {
  appForm: FormGroup;
  loading$: Observable<boolean>;

  constructor(private fb: FormBuilder, private store: Store) {
    this.appForm = this.fb.group({
      builderName: ['', [Validators.required, Validators.minLength(2)]],
      projectName: ['', [Validators.required, Validators.minLength(2)]],
      unitNumber: ['', [Validators.required]],
      propertyValue: ['', [Validators.required, Validators.min(100000)]],
      purchaseDate: ['', [Validators.required]],
      promisedPossessionDate: ['', [Validators.required]]
    });
    this.loading$ = this.store.select(selectApplicationLoading);
  }

  onSubmit(): void {
    if (this.appForm.valid) {
      this.store.dispatch(
        ApplicationActions.createApplication({
          request: {
            ...this.appForm.value,
            propertyValue: Number(this.appForm.value.propertyValue)
          }
        })
      );
    } else {
      this.appForm.markAllAsTouched();
    }
  }
}
