import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { CurrencyFormatterDirective } from './currency-formatter.directive';

@Component({
  standalone: true,
  imports: [CurrencyFormatterDirective],
  template: `<input type="text" id="curInput" appCurrencyFormatter value="5000000">`
})
class TestHostComponent {}

describe('CurrencyFormatterDirective', () => {
  let fixture: ComponentFixture<TestHostComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TestHostComponent]
    });
    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it('should format input on init', () => {
    const inputEl = fixture.debugElement.query(By.css('#curInput')).nativeElement;
    expect(inputEl.value).toContain('50,00,000');
  });
});
