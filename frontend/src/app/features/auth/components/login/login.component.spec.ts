import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { LoginComponent } from './login.component';
import * as AuthActions from '../../state/auth.actions';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent, ReactiveFormsModule],
      providers: [
        provideMockStore(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParams: {} } }
        }
      ]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize form as invalid when empty', () => {
    expect(component.loginForm.valid).toBeFalse();
  });

  it('should dispatch login action when valid', () => {
    spyOn(store, 'dispatch');
    component.loginForm.setValue({
      email: 'buyer@test.com',
      password: 'Password123'
    });
    component.onSubmit();
    expect(store.dispatch).toHaveBeenCalledWith(
      AuthActions.login({
        request: { email: 'buyer@test.com', password: 'Password123' },
        returnUrl: undefined
      })
    );
  });
});
