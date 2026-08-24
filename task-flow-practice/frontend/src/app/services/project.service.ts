import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Project, ProjectCreateRequest } from '../models/project.model';
import { PageResponse } from '../models/page-response.model';

@Injectable({ providedIn: 'root' })
export class ProjectService {
  private http = inject(HttpClient);
  private baseUrl = '/api/projects';

  getProjects(): Observable<Project[]> {
    return this.http.get<PageResponse<Project>>(this.baseUrl).pipe(
      map(r => r.data)
    );
  }

  createProject(data: ProjectCreateRequest): Observable<Project> {
    return this.http.post<Project>(this.baseUrl, data);
  }

  updateProject(id: number, data: ProjectCreateRequest): Observable<Project> {
    return this.http.put<Project>(`${this.baseUrl}/${id}`, data);
  }

  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
