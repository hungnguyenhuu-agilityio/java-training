import { Component, OnInit, ViewChildren, QueryList, inject, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Task, TaskStatus } from '../../models/task.model';
import { TaskService } from '../../services/task.service';
import { KanbanColumnComponent } from '../kanban-column/kanban-column.component';

@Component({
  selector: 'app-kanban',
  standalone: true,
  imports: [CommonModule, KanbanColumnComponent],
  templateUrl: './kanban.component.html',
  styleUrls: ['./kanban.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class KanbanComponent implements OnInit {
  private taskService = inject(TaskService);

  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly tasks = signal<Task[]>([]);

  readonly todoTasks = computed(() => this.tasks().filter(t => t.status === 'TODO'));
  readonly inProgressTasks = computed(() => this.tasks().filter(t => t.status === 'IN_PROGRESS'));
  readonly doneTasks = computed(() => this.tasks().filter(t => t.status === 'DONE'));

  @ViewChildren(KanbanColumnComponent) columns!: QueryList<KanbanColumnComponent>;

  ngOnInit(): void {
    this.loading.set(true);
    this.taskService.getTasks().subscribe({
      next: tasks => {
        this.tasks.set(tasks);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to load tasks.');
        this.loading.set(false);
      }
    });
  }

  onStatusChanged(event: { task: Task; nextStatus: TaskStatus }): void {
    const { task, nextStatus } = event;
    const prevStatus = task.status;

    const current = this.tasks();
    const idx = current.findIndex(t => t.id === task.id);
    if (idx === -1) return;

    // Optimistic update
    const updated = [...current];
    updated[idx] = { ...updated[idx], status: nextStatus };
    this.tasks.set(updated);

    this.taskService.updateTask(task.id, { ...task, status: nextStatus }).subscribe({
      next: serverTask => {
        const arr = [...this.tasks()];
        arr[idx] = serverTask;
        this.tasks.set(arr);
        this.errorMessage.set('');
      },
      error: () => {
        const arr = [...this.tasks()];
        arr[idx] = { ...arr[idx], status: prevStatus };
        this.tasks.set(arr);
        this.errorMessage.set(`Failed to update task "${task.title}". Please try again.`);
        this.columns.forEach(col => col.resetCard(task.id));
      }
    });
  }
}
