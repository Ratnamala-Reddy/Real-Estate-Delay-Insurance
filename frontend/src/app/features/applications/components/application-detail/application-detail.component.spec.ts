import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { ApplicationDetailComponent } from './application-detail.component';
import * as ApplicationActions from '../../state/application.actions';

describe('ApplicationDetailComponent', () => {
  let component: ApplicationDetailComponent;
  let fixture: ComponentFixture<ApplicationDetailComponent>;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationDetailComponent, ReactiveFormsModule, RouterModule.forRoot([])],
      providers: [
        provideMockStore(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'APP-999' } } }
        }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(ApplicationDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should dispatch loadApplicationById and loadDocuments on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(ApplicationActions.loadApplicationById({ applicationId: 'APP-999' }));
    expect(store.dispatch).toHaveBeenCalledWith(ApplicationActions.loadDocuments({ applicationId: 'APP-999' }));
  });
});
