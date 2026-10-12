import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore } from '@ngrx/store/testing';
import { RouterModule } from '@angular/router';
import { HomeComponent } from './home.component';
import { selectIsAuthenticated, selectUserRole, selectCurrentUser } from '../../../features/auth/state/auth.selectors';

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent, RouterModule.forRoot([])],
      providers: [
        provideMockStore({
          selectors: [
            { selector: selectIsAuthenticated, value: true },
            { selector: selectUserRole, value: 'BUYER' },
            { selector: selectCurrentUser, value: { firstName: 'Laxmi', lastName: 'Reddy', role: 'BUYER' } }
          ]
        })
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create home component', () => {
    expect(component).toBeTruthy();
  });
});
