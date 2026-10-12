import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectAllApplications, selectApplicationLoading } from '../../state/application.selectors';
import * as ApplicationActions from '../../state/application.actions';
import { Application } from '../../state/application.models';

@Component({
  selector: 'app-application-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './application-list.component.html',
  styleUrls: ['./application-list.component.css']
})
export class ApplicationListComponent implements OnInit {
  applications$: Observable<Application[]>;
  loading$: Observable<boolean>;

  constructor(private store: Store) {
    this.applications$ = this.store.select(selectAllApplications);
    this.loading$ = this.store.select(selectApplicationLoading);
  }

  ngOnInit(): void {
    this.store.dispatch(ApplicationActions.loadMyApplications());
  }
}
