import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RouterModule } from '@angular/router';
import { ApplicationListComponent } from './application-list.component';
import * as ApplicationActions from '../../state/application.actions';
import { selectAllApplications, selectApplicationLoading } from '../../state/application.selectors';

describe('ApplicationListComponent', () => {
  let component: ApplicationListComponent;
  let fixture: ComponentFixture<ApplicationListComponent>;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationListComponent, RouterModule.forRoot([])],
      providers: [
        provideMockStore({
          selectors: [
            { selector: selectAllApplications, value: [] },
            { selector: selectApplicationLoading, value: false }
          ]
        })
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(ApplicationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should dispatch loadMyApplications on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(ApplicationActions.loadMyApplications());
  });
});
