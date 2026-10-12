import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { UserProfileResponse, UserStatusUpdateRequest } from '../../auth/state/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<UserProfileResponse[]> {
    return this.http.get<UserProfileResponse[]>(`${this.baseUrl}/users`);
  }

  updateUserStatus(userId: number, request: UserStatusUpdateRequest): Observable<any> {
    return this.http.patch<any>(`${this.baseUrl}/users/${userId}/status`, request);
  }
}
