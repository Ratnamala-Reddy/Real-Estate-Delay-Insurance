import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { ProfileComponent } from './profile.component';
import * as AuthActions from '../../state/auth.actions';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileComponent, ReactiveFormsModule],
      providers: [provideMockStore()]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should dispatch loadProfile on init', () => {
    spyOn(store, 'dispatch');
    component.ngOnInit();
    expect(store.dispatch).toHaveBeenCalledWith(AuthActions.loadProfile());
  });
});
