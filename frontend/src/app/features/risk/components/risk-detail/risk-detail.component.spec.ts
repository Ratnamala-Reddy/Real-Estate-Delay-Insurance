import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RiskDetailComponent } from './risk-detail.component';
import * as RiskActions from '../../state/risk.actions';

describe('RiskDetailComponent', () => {
  let component: RiskDetailComponent;
  let fixture: ComponentFixture<RiskDetailComponent>;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RiskDetailComponent, RouterModule.forRoot([])],
      providers: [
        provideMockStore(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'APP-500' } } }
        }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(RiskDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should dispatch loadRiskAssessment on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(RiskActions.loadRiskAssessment({ applicationId: 'APP-500' }));
  });
});
