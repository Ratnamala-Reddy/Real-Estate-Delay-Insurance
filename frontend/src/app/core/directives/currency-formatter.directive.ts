import { Directive, ElementRef, HostListener, OnInit } from '@angular/core';

@Directive({
  selector: '[appCurrencyFormatter]',
  standalone: true
})
export class CurrencyFormatterDirective implements OnInit {
  constructor(private el: ElementRef) {}

  ngOnInit(): void {
    this.format();
  }

  @HostListener('blur')
  onBlur(): void {
    this.format();
  }

  private format(): void {
    const val = this.el.nativeElement.value;
    if (val !== null && val !== undefined && val !== '') {
      const numeric = parseFloat(val.toString().replace(/[^0-9.-]+/g, ''));
      if (!isNaN(numeric)) {
        this.el.nativeElement.value = new Intl.NumberFormat('en-IN', {
          style: 'currency',
          currency: 'INR',
          maximumFractionDigits: 0
        }).format(numeric);
      }
    }
  }
}
