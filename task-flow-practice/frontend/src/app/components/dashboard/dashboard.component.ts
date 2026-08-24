import { Component, OnInit, inject, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatsService } from '../../services/stats.service';
import { DashboardStats } from '../../models/stats.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
  private statsService = inject(StatsService);

  readonly loading = signal(true);
  readonly error = signal('');
  readonly stats = signal<DashboardStats | null>(null);

  readonly statusKeys = ['TODO', 'IN_PROGRESS', 'DONE'];
  readonly priorityKeys = ['LOW', 'MEDIUM', 'HIGH'];

  readonly totalTasks = computed(() => {
    const s = this.stats();
    if (!s) return 0;
    return Object.values(s.byStatus).reduce((sum, n) => sum + n, 0);
  });

  readonly maxStatusCount = computed(() => {
    const s = this.stats();
    if (!s) return 1;
    const vals = this.statusKeys.map(k => s.byStatus[k] ?? 0);
    return Math.max(1, ...vals);
  });

  readonly maxPriorityCount = computed(() => {
    const s = this.stats();
    if (!s) return 1;
    const vals = this.priorityKeys.map(k => s.byPriority[k] ?? 0);
    return Math.max(1, ...vals);
  });

  getPercent(count: number, max: number): number {
    if (max === 0) return 0;
    return Math.round((count / max) * 100);
  }

  ngOnInit(): void {
    this.statsService.getStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load dashboard stats. Please try again.');
        this.loading.set(false);
      }
    });
  }
}
