import { Directive, Input, TemplateRef, ViewContainerRef, OnInit, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { selectUserRole } from '../../features/auth/state/auth.selectors';

@Directive({
  selector: '[appHasRole]',
  standalone: true
})
export class HasRoleDirective implements OnInit, OnDestroy {
  private allowedRoles: string[] = [];
  private currentRole: string | null = null;
  private sub?: Subscription;

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private store: Store
  ) {}

  @Input() set appHasRole(roles: string[] | string) {
    const list = Array.isArray(roles) ? roles : [roles];
    this.allowedRoles = list.map(r => (r || '').trim().toUpperCase());
    this.updateView();
  }

  ngOnInit(): void {
    this.sub = this.store.select(selectUserRole).subscribe((role) => {
      this.currentRole = role;
      this.updateView();
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  private updateView(): void {
    const roleFromStore = this.currentRole;
    const roleFromStorage = typeof localStorage !== 'undefined' ? localStorage.getItem('user_role') : null;
    const activeRole = (roleFromStore || roleFromStorage || '').trim().toUpperCase();

    const isAllowed =
      this.allowedRoles.length === 0 ||
      (!!activeRole && this.allowedRoles.includes(activeRole));

    this.viewContainer.clear();
    if (isAllowed) {
      this.viewContainer.createEmbeddedView(this.templateRef);
    }
  }
}
