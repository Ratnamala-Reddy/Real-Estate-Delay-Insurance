import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { RegisterComponent } from './register.component';
import * as AuthActions from '../../state/auth.actions';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterComponent, ReactiveFormsModule],
      providers: [provideMockStore(), provideRouter([])]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should validate 10-digit mobile number', () => {
    const mobileCtrl = component.registerForm.get('mobileNumber');
    mobileCtrl?.setValue('12345');
    expect(mobileCtrl?.valid).toBeFalse();
    mobileCtrl?.setValue('9876543210');
    expect(mobileCtrl?.valid).toBeTrue();
  });

  it('should dispatch register action on submit', () => {
    spyOn(store, 'dispatch');
    component.registerForm.setValue({
      firstName: 'Jane',
      lastName: 'Doe',
      email: 'jane@test.com',
      mobileNumber: '9988776655',
      password: 'SecurePassword123'
    });
    component.onSubmit();
    expect(store.dispatch).toHaveBeenCalledWith(
      AuthActions.register({ request: component.registerForm.value })
    );
  });
});
