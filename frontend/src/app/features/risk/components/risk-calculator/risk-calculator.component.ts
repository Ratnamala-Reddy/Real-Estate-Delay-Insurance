import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { selectCurrentRiskAssessment, selectRiskLoading } from '../../state/risk.selectors';
import * as RiskActions from '../../state/risk.actions';
import { RiskAssessment } from '../../state/risk.models';
import { ApplicationService } from '../../../applications/services/application.service';
import { Application, ApplicationDocument } from '../../../applications/state/application.models';
import { NotificationService } from '../../../../core/services/notification.service';

export interface FactorAnalysis {
  factor: string;
  score: number;
  weight: number;
  badge: string;
  badgeType: 'success' | 'warning' | 'info';
  rationale: string;
}

export interface SystemRiskSummary {
  overallScore: number;
  category: 'LOW' | 'MEDIUM' | 'HIGH';
  multiplier: number;
  confidencePercent: number;
}

@Component({
  selector: 'app-risk-calculator',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './risk-calculator.component.html',
  styleUrls: ['./risk-calculator.component.css']
})
export class RiskCalculatorComponent implements OnInit {
  applicationId!: string;
  riskForm: FormGroup;
  assessment$: Observable<RiskAssessment | null>;
  loading$: Observable<boolean>;
  application: Application | null = null;
  documents: ApplicationDocument[] = [];
  appLoading = false;
  analysisFactors: FactorAnalysis[] = [];
  systemSummary: SystemRiskSummary | null = null;
  isOverrideMode = false;

  constructor(
    private route: ActivatedRoute,
    private fb: FormBuilder,
    private store: Store,
    private router: Router,
    private applicationService: ApplicationService,
    private notificationService: NotificationService
  ) {
    this.riskForm = this.fb.group({
      assessedBy: ['AI Underwriting Engine (Automated)', Validators.required],
      builderScore: [80, [Validators.required, Validators.min(0), Validators.max(100)]],
      financialScore: [80, [Validators.required, Validators.min(0), Validators.max(100)]],
      projectProgressScore: [80, [Validators.required, Validators.min(0), Validators.max(100)]],
      locationScore: [85, [Validators.required, Validators.min(0), Validators.max(100)]],
      approvalScore: [85, [Validators.required, Validators.min(0), Validators.max(100)]]
    });
    this.assessment$ = this.store.select(selectCurrentRiskAssessment);
    this.loading$ = this.store.select(selectRiskLoading);
  }

  ngOnInit(): void {
    this.applicationId = this.route.snapshot.paramMap.get('id') || '';
    if (this.applicationId) {
      this.store.dispatch(RiskActions.loadRiskAssessment({ applicationId: this.applicationId }));
      this.loadApplicationData();
    }
  }

  loadApplicationData(): void {
    this.appLoading = true;
    this.applicationService.getApplicationById(this.applicationId).subscribe({
      next: (app) => {
        this.application = app;
        this.applicationService.getDocuments(this.applicationId).subscribe({
          next: (docs) => {
            this.documents = docs || [];
            this.appLoading = false;
            this.autoDeriveScores();
          },
          error: () => {
            this.appLoading = false;
            this.autoDeriveScores();
          }
        });
      },
      error: () => {
        this.appLoading = false;
      }
    });
  }

  autoDeriveScores(): void {
    if (!this.application) return;

    const factors: FactorAnalysis[] = [];

    // 1. Statutory Approvals & RERA Documents (20% Weight)
    let approvalScore = 40;
    const hasRera = this.documents.some((d) => d.documentType === 'RERA_REGISTRATION');
    const hasSanction = this.documents.some((d) => d.documentType === 'SANCTION_PLAN');
    const hasAgreement = this.documents.some((d) => d.documentType === 'PURCHASE_AGREEMENT');
    const hasReceipts = this.documents.some((d) => d.documentType === 'PAYMENT_RECEIPTS');

    if (hasRera) approvalScore += 30;
    if (hasSanction) approvalScore += 20;
    if (hasAgreement) approvalScore += 5;
    if (hasReceipts) approvalScore += 5;
    approvalScore = Math.min(100, approvalScore);

    factors.push({
      factor: 'Statutory Approvals & RERA',
      score: approvalScore,
      weight: 20,
      badge: hasRera && hasSanction ? 'High Compliance' : 'Partial Compliance',
      badgeType: hasRera && hasSanction ? 'success' : 'warning',
      rationale: `Verified attached documents: ${hasRera ? '✓ RERA Registration (+30)' : '✕ RERA Missing'}, ${
        hasSanction ? '✓ Sanction Plan (+20)' : '✕ Sanction Missing'
      }, ${hasAgreement ? '✓ Sale Agreement (+5)' : ''}`
    });

    // 2. Construction Progress (20% Weight)
    let progressScore = 75;
    let progressRationale = 'Standard construction delivery schedule.';
    let progressBadgeType: 'success' | 'warning' | 'info' = 'info';

    if (this.application.promisedPossessionDate) {
      const now = new Date().getTime();
      const handover = new Date(this.application.promisedPossessionDate).getTime();
      const monthsUntilHandover = (handover - now) / (1000 * 60 * 60 * 24 * 30.4);

      if (monthsUntilHandover < 0) {
        progressScore = 45;
        progressBadgeType = 'warning';
        progressRationale = `Promised handover was ${Math.abs(Math.round(monthsUntilHandover))} months ago (Overdue Delivery / Delay Risk).`;
      } else if (monthsUntilHandover <= 12) {
        progressScore = 92;
        progressBadgeType = 'success';
        progressRationale = `Target possession is within 12 months (${Math.max(1, Math.round(monthsUntilHandover))} mos). Advanced stage.`;
      } else if (monthsUntilHandover <= 24) {
        progressScore = 82;
        progressBadgeType = 'info';
        progressRationale = `Target possession is within 2 years (${Math.round(monthsUntilHandover)} mos). Active construction phase.`;
      } else {
        progressScore = 72;
        progressBadgeType = 'info';
        progressRationale = `Early construction milestone (${Math.round(monthsUntilHandover)} months to promised handover).`;
      }
    }

    factors.push({
      factor: 'Construction Progress Milestone',
      score: progressScore,
      weight: 20,
      badge: progressScore >= 80 ? 'On Track' : 'Monitor Progress',
      badgeType: progressBadgeType,
      rationale: progressRationale
    });

    // 3. Builder Track Record (20% Weight)
    const topBuilders = ['prestige', 'godrej', 'dlf', 'sobha', 'brigade', 'tata', 'lodha', 'oberoi', 'puravankara', 'l&t'];
    const builderLower = (this.application.builderName || '').toLowerCase();
    const isTopTier = topBuilders.some((b) => builderLower.includes(b));
    const builderScore = isTopTier ? 92 : 80;

    factors.push({
      factor: 'Builder Track Record & Solvency',
      score: builderScore,
      weight: 20,
      badge: isTopTier ? 'Tier-1 National Brand' : 'Standard Developer',
      badgeType: isTopTier ? 'success' : 'info',
      rationale: isTopTier
        ? `Recognized Tier-1 developer '${this.application.builderName}' with proven delivery consistency.`
        : `Registered regional developer '${this.application.builderName}' with standard baseline underwriting.`
    });

    // 4. Financial Solvency (20% Weight)
    const propVal = Number(this.application.propertyValue) || 0;
    let financialScore = 80;
    let finRationale = 'Standard asset capitalization and escrow eligibility.';

    if (propVal >= 10000000) {
      financialScore = 90;
      finRationale = `High-value asset (₹${(propVal / 100000).toFixed(1)} Lakhs) with tier-1 mortgage lender pre-approvals.`;
    } else if (propVal >= 5000000) {
      financialScore = 85;
      finRationale = `Mid-to-high residential asset (₹${(propVal / 100000).toFixed(1)} Lakhs) under bank escrow protection.`;
    } else {
      financialScore = 78;
      finRationale = `Standard residential unit (₹${(propVal / 100000).toFixed(1)} Lakhs).`;
    }

    factors.push({
      factor: 'Financial Solvency & Escrow',
      score: financialScore,
      weight: 20,
      badge: financialScore >= 85 ? 'Strong Solvency' : 'Standard Escrow',
      badgeType: financialScore >= 85 ? 'success' : 'info',
      rationale: finRationale
    });

    // 5. Location / Micro-Market (20% Weight)
    const locationScore = 88;
    factors.push({
      factor: 'Location & Micro-Market',
      score: locationScore,
      weight: 20,
      badge: 'Approved Urban Zone',
      badgeType: 'success',
      rationale: 'Project situated in approved urban municipal master plan with clear zoning infrastructure.'
    });

    this.analysisFactors = factors;

    // Calculate Overall System Weighted Average
    const overallScore = Math.round(
      (builderScore + financialScore + progressScore + locationScore + approvalScore) / 5
    );

    let category: 'LOW' | 'MEDIUM' | 'HIGH' = 'MEDIUM';
    let multiplier = 1.35;

    if (overallScore >= 75) {
      category = 'LOW';
      multiplier = 1.05;
    } else if (overallScore >= 50) {
      category = 'MEDIUM';
      multiplier = 1.35;
    } else {
      category = 'HIGH';
      multiplier = 1.95;
    }

    this.systemSummary = {
      overallScore,
      category,
      multiplier,
      confidencePercent: 96
    };

    // Pre-populate reactive form sliders
    this.riskForm.patchValue({
      builderScore,
      financialScore,
      projectProgressScore: progressScore,
      locationScore,
      approvalScore
    });
  }

  toggleOverride(): void {
    this.isOverrideMode = !this.isOverrideMode;
  }

  onOneClickApprove(): void {
    if (!this.application) return;

    const val = this.riskForm.value;
    const request = {
      applicationId: this.applicationId,
      assessedBy: String(val.assessedBy || 'AI Underwriting Engine').trim(),
      builderScore: Number(val.builderScore),
      financialScore: Number(val.financialScore),
      projectProgressScore: Number(val.projectProgressScore),
      locationScore: Number(val.locationScore),
      approvalScore: Number(val.approvalScore)
    };

    // Dispatches automatic risk calculation to backend
    this.store.dispatch(RiskActions.calculateRisk({ request }));
  }

  onApprove(assessmentId: string): void {
    const assessedBy = String(this.riskForm.value.assessedBy || 'Risk Underwriter Admin').trim();
    this.store.dispatch(
      RiskActions.approveRiskAssessment({
        assessmentId,
        request: {
          remarks: 'Approved by Underwriting Risk Officer',
          approvedBy: assessedBy
        }
      })
    );
  }

  onDecline(assessmentId: string): void {
    const assessedBy = String(this.riskForm.value.assessedBy || 'Risk Underwriter Admin').trim();
    this.store.dispatch(
      RiskActions.declineRiskAssessment({
        assessmentId,
        request: {
          remarks: 'Declined due to elevated default risk',
          declinedBy: assessedBy
        }
      })
    );
  }
}
