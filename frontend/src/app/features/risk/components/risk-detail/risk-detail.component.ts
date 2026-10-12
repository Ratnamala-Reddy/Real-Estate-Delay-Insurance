import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectCurrentRiskAssessment, selectRiskLoading } from '../../state/risk.selectors';
import * as RiskActions from '../../state/risk.actions';
import { RiskAssessment } from '../../state/risk.models';

@Component({
  selector: 'app-risk-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './risk-detail.component.html',
  styleUrls: ['./risk-detail.component.css']
})
export class RiskDetailComponent implements OnInit {
  applicationId!: string;
  assessment$: Observable<RiskAssessment | null>;
  loading$: Observable<boolean>;

  constructor(
    private route: ActivatedRoute,
    private store: Store
  ) {
    this.assessment$ = this.store.select(selectCurrentRiskAssessment);
    this.loading$ = this.store.select(selectRiskLoading);
  }

  ngOnInit(): void {
    this.applicationId = this.route.snapshot.paramMap.get('id') || '';
    if (this.applicationId) {
      this.store.dispatch(RiskActions.loadRiskAssessment({ applicationId: this.applicationId }));
    }
  }
}
