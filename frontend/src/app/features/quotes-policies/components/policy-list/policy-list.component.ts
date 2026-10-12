import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectAllPolicies, selectPolicyLoading } from '../../state/policy.selectors';
import * as PolicyActions from '../../state/policy.actions';
import { Policy } from '../../state/policy.models';

import { selectUserRole } from '../../../auth/state/auth.selectors';
import { map } from 'rxjs';

@Component({
  selector: 'app-policy-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './policy-list.component.html',
  styleUrls: ['./policy-list.component.css']
})
export class PolicyListComponent implements OnInit {
  policies$: Observable<Policy[]>;
  loading$: Observable<boolean>;
  isBuyer$: Observable<boolean>;

  constructor(private store: Store) {
    this.policies$ = this.store.select(selectAllPolicies);
    this.loading$ = this.store.select(selectPolicyLoading);
    this.isBuyer$ = this.store.select(selectUserRole).pipe(map((r) => r === 'BUYER'));
  }

  ngOnInit(): void {
    this.store.dispatch(PolicyActions.loadPolicies());
  }
}
