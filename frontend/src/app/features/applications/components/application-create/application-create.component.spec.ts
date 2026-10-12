import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { ApplicationCreateComponent } from './application-create.component';
import * as ApplicationActions from '../../state/application.actions';

describe('ApplicationCreateComponent', () => {
  let component: ApplicationCreateComponent;
  let fixture: ComponentFixture<ApplicationCreateComponent>;
  let store: MockStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationCreateComponent, ReactiveFormsModule, RouterModule.forRoot([])],
      providers: [provideMockStore()]
    }).compileComponents();

    store = TestBed.inject(MockStore);
    fixture = TestBed.createComponent(ApplicationCreateComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize invalid form', () => {
    expect(component.appForm.valid).toBeFalse();
  });

  it('should dispatch createApplication action on valid submit', () => {
    spyOn(store, 'dispatch');
    component.appForm.setValue({
      builderName: 'Brigade',
      projectName: 'Orchards',
      unitNumber: 'Tower 1, 202',
      propertyValue: 8500000,
      purchaseDate: '2024-03-01',
      promisedPossessionDate: '2026-06-30'
    });
    component.onSubmit();
    expect(store.dispatch).toHaveBeenCalled();
  });
});
