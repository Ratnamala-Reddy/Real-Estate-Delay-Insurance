import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title: string;
  message: string;
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
  public toasts$: Observable<ToastMessage[]> = this.toastsSubject.asObservable();

  show(type: 'success' | 'error' | 'info' | 'warning', title: string, message: string, duration: number = 3200): void {
    if (!message || message.trim() === '') return;

    const current = this.toastsSubject.value;
    
    // Deduplicate identical message
    const isDuplicate = current.some(
      (t) => t.message.toLowerCase() === message.toLowerCase() && t.type === type
    );
    if (isDuplicate) {
      return;
    }

    const id = Math.random().toString(36).substring(2, 9);
    const toast: ToastMessage = { id, type, title, message, duration };
    
    // Keep max 2 active toasts to avoid corner popup spam
    const updated = [...current.slice(-1), toast];
    this.toastsSubject.next(updated);

    if (duration > 0) {
      setTimeout(() => this.remove(id), duration);
    }
  }

  success(title: string, message: string): void {
    this.show('success', title, message, 3000);
  }

  error(title: string, message: string): void {
    this.show('error', title, message, 4500);
  }

  info(title: string, message: string): void {
    this.show('info', title, message, 3000);
  }

  warning(title: string, message: string): void {
    this.show('warning', title, message, 3500);
  }

  remove(id: string): void {
    const current = this.toastsSubject.value;
    this.toastsSubject.next(current.filter(t => t.id !== id));
  }
}
