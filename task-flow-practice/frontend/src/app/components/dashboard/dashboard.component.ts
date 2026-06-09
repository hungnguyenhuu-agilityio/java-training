import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { StatsService } from '../../services/stats.service';
import { DashboardStats } from '../../models/stats.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  private authService = inject(AuthService);
  private statsService = inject(StatsService);

  stats: DashboardStats | null = null;
  loading = true;
  error = '';

  readonly statusKeys = ['TODO', 'IN_PROGRESS', 'DONE'];
  readonly priorityKeys = ['LOW', 'MEDIUM', 'HIGH'];

  get totalTasks(): number {
    if (!this.stats) return 0;
    return Object.values(this.stats.byStatus).reduce((sum, n) => sum + n, 0);
  }

  get maxStatusCount(): number {
    if (!this.stats) return 1;
    const vals = this.statusKeys.map(k => this.stats!.byStatus[k] ?? 0);
    return Math.max(1, ...vals);
  }

  get maxPriorityCount(): number {
    if (!this.stats) return 1;
    const vals = this.priorityKeys.map(k => this.stats!.byPriority[k] ?? 0);
    return Math.max(1, ...vals);
  }

  getPercent(count: number, max: number): number {
    if (max === 0) return 0;
    return Math.round((count / max) * 100);
  }

  ngOnInit(): void {
    this.statsService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load dashboard stats. Please try again.';
        this.loading = false;
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}
