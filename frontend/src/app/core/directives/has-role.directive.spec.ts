import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideMockStore, MockStore } from '@ngrx/store/testing';
import { HasRoleDirective } from './has-role.directive';
import { selectUserRole } from '../../features/auth/state/auth.selectors';

@Component({
  standalone: true,
  imports: [HasRoleDirective],
  template: `<div *appHasRole="['ADMIN']" id="adminContent">Admin Area</div>`
})
class TestHostComponent {}

describe('HasRoleDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let store: MockStore;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideMockStore()]
    });
    store = TestBed.inject(MockStore);
  });

  it('should render content if role matches', () => {
    localStorage.setItem('user_role', 'ADMIN');
    store.overrideSelector(selectUserRole, 'ADMIN');
    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    const el = fixture.debugElement.query(By.css('#adminContent'));
    expect(el).toBeTruthy();
  });
});
