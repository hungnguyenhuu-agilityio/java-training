import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';

const TOKEN_KEY = 'taskflow_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  // Reactive signal so OnPush components re-render without polling
  readonly loggedIn = signal<boolean>(!!localStorage.getItem(TOKEN_KEY));

  login(email: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>('/api/auth/login', { email, password });
  }

  register(name: string, email: string, password: string): Observable<any> {
    return this.http.post<any>('/api/auth/register', { name, email, password });
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.loggedIn.set(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  storeToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.loggedIn.set(true);
  }
}
