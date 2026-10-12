import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { NotificationService } from '../services/notification.service';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

  private sanitizeUserFacingError(rawMsg: string, status: number): string {
    if (!rawMsg) {
      if (status === 409) return 'A conflicting record already exists for this submission.';
      if (status === 400) return 'Invalid request data. Please check your inputs.';
      if (status === 403) return 'You do not have permission to perform this action.';
      if (status === 404) return 'The requested resource was not found.';
      return 'An unexpected system error occurred. Please try again.';
    }

    // Database Duplicate Key & Constraints
    if (rawMsg.includes('uk_claim_policy') || (rawMsg.includes('Duplicate entry') && rawMsg.includes('claim'))) {
      return 'A claim has already been submitted for this policy ID. Each insurance policy can have only one claim.';
    }

    if (rawMsg.includes('Duplicate entry') || rawMsg.includes('uk_') || rawMsg.includes('DuplicateKeyException')) {
      return 'A record with this information already exists in the system. Duplicate submissions are not allowed.';
    }

    // Raw SQL / Hibernate / R2DBC errors
    if (rawMsg.includes('SQL [') || rawMsg.includes('executeMany') || rawMsg.includes('r2dbc') || rawMsg.includes('MySqlDataRow') || rawMsg.includes('DataIntegrityViolation')) {
      return 'A database conflict occurred while processing your request. Please verify your details.';
    }

    // Network / connection drop
    if (rawMsg.includes('Connection refused') || rawMsg.includes('Failed to connect') || rawMsg.includes('Http failure response')) {
      return 'The requested insurance service is temporarily offline. Please verify that backend services are active.';
    }

    return rawMsg;
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        let rawMessage = 'An unexpected error occurred.';
        if (error.error && typeof error.error === 'object' && error.error.message) {
          rawMessage = error.error.message;
        } else if (typeof error.error === 'string') {
          rawMessage = error.error;
        } else if (error.statusText) {
          rawMessage = error.statusText;
        }

        const errorMessage = this.sanitizeUserFacingError(rawMessage, error.status);

        if (error.status === 401) {
          if (req.url.includes('/api/v1/auth/login')) {
            this.notificationService.error('Login Failed', errorMessage || 'Invalid email or password.');
          } else {
            const hasToken = !!localStorage.getItem('token');
            if (hasToken) {
              this.notificationService.error('Session Expired', 'Please login to continue.');
              this.router.navigate(['/auth/login']);
            }
          }
        } else if (error.status === 403) {
          this.notificationService.error('Access Denied', errorMessage || 'You do not have permission to perform this action.');
        } else if (error.status === 409) {
          this.notificationService.warning('Submission Conflict', errorMessage);
        } else if (error.status === 404) {
          // Do NOT show toast notifications for standard GET resource lookups (e.g. checking if risk assessment, quote, or verification exists)
          if (req.method !== 'GET') {
            this.notificationService.warning('Not Found', errorMessage || 'Requested item was not found.');
          }
        } else if (error.status === 400) {
          this.notificationService.error('Validation Error', errorMessage);
        } else if (error.status >= 500) {
          if (!req.url.includes('/api/v1/premium-payments')) {
            this.notificationService.error('Service Error', errorMessage || 'Microservice temporarily unavailable. Please retry shortly.');
          }
        }

        return throwError(() => error);
      })
    );
  }
}
