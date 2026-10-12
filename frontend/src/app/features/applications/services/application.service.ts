import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Application,
  CreateApplicationRequest,
  ApplicationDocument,
  UploadDocumentRequest,
  ApplicationStatusUpdateRequest
} from '../state/application.models';

@Injectable({
  providedIn: 'root'
})
export class ApplicationService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/v1/applications`;

  constructor(private http: HttpClient) {}

  getMyApplications(): Observable<Application[]> {
    return this.http.get<Application[]>(this.baseUrl);
  }

  getAllApplications(): Observable<Application[]> {
    return this.http.get<Application[]>(`${this.baseUrl}/all`);
  }

  getApplicationById(applicationId: string): Observable<Application> {
    return this.http.get<Application>(`${this.baseUrl}/${applicationId}`);
  }

  createApplication(request: CreateApplicationRequest): Observable<Application> {
    return this.http.post<Application>(this.baseUrl, request);
  }

  updateApplication(applicationId: string, request: CreateApplicationRequest): Observable<Application> {
    return this.http.put<Application>(`${this.baseUrl}/${applicationId}`, request);
  }

  updateStatus(applicationId: string, status: string): Observable<Application> {
    return this.http.patch<Application>(`${this.baseUrl}/${applicationId}/status`, { status });
  }

  submitApplication(applicationId: string): Observable<Application> {
    return this.http.post<Application>(`${this.baseUrl}/${applicationId}/submit`, {});
  }

  withdrawApplication(applicationId: string): Observable<Application> {
    return this.http.post<Application>(`${this.baseUrl}/${applicationId}/withdraw`, {});
  }

  getDocuments(applicationId: string): Observable<ApplicationDocument[]> {
    return this.http.get<ApplicationDocument[]>(`${this.baseUrl}/${applicationId}/documents`);
  }

  uploadDocument(applicationId: string, request: UploadDocumentRequest): Observable<ApplicationDocument> {
    return this.http.post<ApplicationDocument>(`${this.baseUrl}/${applicationId}/documents`, request);
  }
}
