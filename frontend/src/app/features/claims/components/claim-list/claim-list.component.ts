import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable, map, of, catchError } from 'rxjs';
import { selectAllClaims, selectClaimLoading } from '../../state/claim.selectors';
import { selectUserRole } from '../../../auth/state/auth.selectors';
import * as ClaimActions from '../../state/claim.actions';
import { Claim } from '../../state/claim.models';
import { PaymentService } from '../../../payments/services/payment.service';

@Component({
  selector: 'app-claim-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './claim-list.component.html',
  styleUrls: ['./claim-list.component.css']
})
export class ClaimListComponent implements OnInit {
  claims$: Observable<Claim[]>;
  loading$: Observable<boolean>;
  userRole$: Observable<string | null>;
  isAdmin$: Observable<boolean>;
  isBuyer$: Observable<boolean>;
  compensationPayments$: Observable<any[]>;

  constructor(
    private store: Store,
    private paymentService: PaymentService
  ) {
    this.claims$ = this.store.select(selectAllClaims);
    this.loading$ = this.store.select(selectClaimLoading);
    this.userRole$ = this.store.select(selectUserRole);
    this.isAdmin$ = this.userRole$.pipe(map((role) => role === 'ADMIN'));
    this.isBuyer$ = this.userRole$.pipe(map((role) => role === 'BUYER'));
    this.compensationPayments$ = this.paymentService.getAllCompensationPayments().pipe(
      catchError((err) => {
        console.warn('Could not load compensation payments:', err);
        return of([]);
      })
    );
  }

  ngOnInit(): void {
    this.store.dispatch(ClaimActions.loadClaims());
  }
}
