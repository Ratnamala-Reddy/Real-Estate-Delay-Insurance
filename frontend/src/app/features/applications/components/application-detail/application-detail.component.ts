import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectSelectedApplication, selectApplicationDocuments, selectApplicationLoading } from '../../state/application.selectors';
import * as ApplicationActions from '../../state/application.actions';
import { Application, ApplicationDocument } from '../../state/application.models';
import { selectUserRole } from '../../../auth/state/auth.selectors';

@Component({
  selector: 'app-application-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './application-detail.component.html',
  styleUrls: ['./application-detail.component.css']
})
export class ApplicationDetailComponent implements OnInit {
  application$: Observable<Application | null>;
  documents$: Observable<ApplicationDocument[]>;
  loading$: Observable<boolean>;
  userRole$: Observable<string | null>;
  docForm: FormGroup;
  applicationId!: string;

  constructor(
    private route: ActivatedRoute,
    private store: Store,
    private fb: FormBuilder
  ) {
    this.application$ = this.store.select(selectSelectedApplication);
    this.documents$ = this.store.select(selectApplicationDocuments);
    this.loading$ = this.store.select(selectApplicationLoading);
    this.userRole$ = this.store.select(selectUserRole);

    this.docForm = this.fb.group({
      documentType: ['PURCHASE_AGREEMENT', Validators.required],
      fileName: ['', Validators.required],
      filePath: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.applicationId = this.route.snapshot.paramMap.get('id') || '';
    if (this.applicationId) {
      this.store.dispatch(ApplicationActions.loadApplicationById({ applicationId: this.applicationId }));
      this.store.dispatch(ApplicationActions.loadDocuments({ applicationId: this.applicationId }));
    }
  }

  onSubmitApplication(): void {
    if (confirm('Are you sure you want to submit this application for verification?')) {
      this.store.dispatch(ApplicationActions.submitApplication({ applicationId: this.applicationId }));
    }
  }

  onWithdraw(): void {
    if (confirm('Are you sure you want to withdraw this application?')) {
      this.store.dispatch(ApplicationActions.withdrawApplication({ applicationId: this.applicationId }));
    }
  }

  onUploadDoc(): void {
    if (this.docForm.valid) {
      this.store.dispatch(
        ApplicationActions.uploadDocument({
          applicationId: this.applicationId,
          request: this.docForm.value
        })
      );
      this.docForm.reset({ documentType: 'PURCHASE_AGREEMENT' });
    }
  }
}
