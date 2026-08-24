import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Task, TaskCreateRequest } from '../models/task.model';
import { PageResponse } from '../models/page-response.model';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private http = inject(HttpClient);
  private baseUrl = '/api/tasks';

  getTasks(params?: HttpParams): Observable<Task[]> {
    return this.http.get<PageResponse<Task>>(this.baseUrl, { params }).pipe(
      map(r => r.data)
    );
  }

  createTask(task: TaskCreateRequest): Observable<Task> {
    return this.http.post<Task>(this.baseUrl, task);
  }

  updateTask(id: number, task: TaskCreateRequest): Observable<Task> {
    return this.http.put<Task>(`${this.baseUrl}/${id}`, task);
  }

  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
