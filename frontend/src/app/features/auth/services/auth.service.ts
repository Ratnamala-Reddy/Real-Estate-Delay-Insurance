import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap, catchError, throwError } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  UserProfileResponse,
  UpdateProfileRequest
} from '../state/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = environment.apiBaseUrl;
  private currentUserSubject = new BehaviorSubject<UserProfileResponse | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/api/v1/auth/login`, request).pipe(
      tap((res) => {
        if (res.role) {
          localStorage.setItem('user_role', res.role);
        }
        if (res.token) {
          localStorage.setItem('access_token', res.token);
        }
      }),
      catchError((err) => throwError(() => err))
    );
  }

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.baseUrl}/api/v1/auth/register`, request);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/v1/auth/logout`, {}).pipe(
      tap(() => {
        localStorage.removeItem('access_token');
        localStorage.removeItem('user_role');
        localStorage.removeItem('user_id');
        this.currentUserSubject.next(null);
      })
    );
  }

  getCurrentUser(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(`${this.baseUrl}/api/v1/users/me`).pipe(
      tap((user) => {
        if (user && user.userId) {
          localStorage.setItem('user_id', user.userId);
        }
        if (user && user.role) {
          localStorage.setItem('user_role', user.role);
        }
        this.currentUserSubject.next(user);
      })
    );
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(`${this.baseUrl}/api/v1/users/me`, request).pipe(
      tap((user) => this.currentUserSubject.next(user))
    );
  }

  getToken(): string | null {
    return localStorage.getItem('access_token');
  }

  getRole(): string | null {
    return localStorage.getItem('user_role');
  }
}
