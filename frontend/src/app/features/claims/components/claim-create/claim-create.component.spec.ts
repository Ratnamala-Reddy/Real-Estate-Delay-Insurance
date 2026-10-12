import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { of } from 'rxjs';
import { ClaimCreateComponent } from './claim-create.component';
import { PolicyService } from '../../../quotes-policies/services/policy.service';

describe('ClaimCreateComponent', () => {
  let component: ClaimCreateComponent;
  let fixture: ComponentFixture<ClaimCreateComponent>;
  let store: MockStore;
  let policyServiceSpy: jasmine.SpyObj<PolicyService>;

  beforeEach(async () => {
    policyServiceSpy = jasmine.createSpyObj('PolicyService', ['getPolicies']);
    policyServiceSpy.getPolicies.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [ClaimCreateComponent, ReactiveFormsModule, RouterModule.forRoot([])],
      providers: [
        provideMockStore(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParams: { policyId: 'POL-123' } } }
        },
        { provide: PolicyService, useValue: policyServiceSpy }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(ClaimCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should patch policyId from route query params', () => {
    expect(component.claimForm.get('policyId')?.value).toBe('POL-123');
  });
});
