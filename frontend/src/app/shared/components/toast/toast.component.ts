import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, ToastMessage } from '../../../core/services/notification.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.css']
})
export class ToastComponent {
  toasts$: Observable<ToastMessage[]>;

  constructor(private notificationService: NotificationService) {
    this.toasts$ = this.notificationService.toasts$;
  }

  close(id: string): void {
    this.notificationService.remove(id);
  }
}
