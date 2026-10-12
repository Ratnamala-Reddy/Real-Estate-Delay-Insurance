import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = localStorage.getItem('access_token');
    const userRole = localStorage.getItem('user_role');
    const userId = localStorage.getItem('user_id');

    let headers = req.headers.set('Accept', 'application/json');
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    if (userRole) {
      headers = headers.set('X-User-Role', userRole);
    }
    if (userId) {
      headers = headers.set('X-User-Id', userId);
    }

    const cloned = req.clone({
      headers,
      withCredentials: true // Enables automatic HttpOnly cookie transmission for Spring Cloud Gateway
    });

    return next.handle(cloned);
  }
}
