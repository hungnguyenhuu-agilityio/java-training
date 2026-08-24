import { Component, OnInit, inject, signal, computed, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpParams } from '@angular/common/http';
import { TaskService } from '../../services/task.service';
import { Task, TaskCreateRequest } from '../../models/task.model';
import { TaskFormComponent } from '../task-form/task-form.component';
import { TaskItemComponent } from '../task-item/task-item.component';
import { FilterBarComponent, FilterParams } from '../filter-bar/filter-bar.component';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, TaskFormComponent, TaskItemComponent, FilterBarComponent],
  templateUrl: './task-list.component.html',
  styleUrls: ['./task-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaskListComponent implements OnInit {
  private taskService = inject(TaskService);

  readonly tasks = signal<Task[]>([]);
  readonly showCreateForm = signal(false);
  readonly editingTask = signal<Task | null>(null);
  readonly loadError = signal('');
  readonly formError = signal('');
  readonly searchTerm = signal('');

  readonly filteredTasks = computed(() => {
    const term = this.searchTerm().toLowerCase();
    if (!term) return this.tasks();
    return this.tasks().filter(t => t.title.toLowerCase().includes(term));
  });

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(params?: HttpParams): void {
    this.loadError.set('');
    this.taskService.getTasks(params).subscribe({
      next: tasks => this.tasks.set(tasks),
      error: () => this.loadError.set('Failed to load tasks. Please try again.')
    });
  }

  onFiltersChanged(filters: FilterParams): void {
    const { searchTerm, ...apiFilters } = filters;
    this.searchTerm.set(searchTerm ?? '');

    const hasApi = !!(apiFilters.status || apiFilters.priority || apiFilters.projectId);

    let params = new HttpParams();
    if (apiFilters.status) params = params.set('status', apiFilters.status);
    if (apiFilters.priority) params = params.set('priority', apiFilters.priority);
    if (apiFilters.projectId) params = params.set('projectId', String(apiFilters.projectId));

    this.loadTasks(hasApi ? params : undefined);
  }

  onCreate(payload: TaskCreateRequest): void {
    this.formError.set('');
    this.taskService.createTask(payload).subscribe({
      next: task => {
        this.tasks.update(list => [...list, task]);
        this.showCreateForm.set(false);
      },
      error: err => {
        this.formError.set(err?.error?.message ?? 'Failed to create task.');
      }
    });
  }

  onUpdate(payload: TaskCreateRequest): void {
    const editing = this.editingTask();
    if (!editing) return;
    this.formError.set('');
    const id = editing.id;
    this.taskService.updateTask(id, payload).subscribe({
      next: updated => {
        this.tasks.update(list => list.map(t => (t.id === id ? updated : t)));
        this.editingTask.set(null);
      },
      error: err => {
        this.formError.set(err?.error?.message ?? 'Failed to update task.');
      }
    });
  }

  onDelete(id: number): void {
    this.taskService.deleteTask(id).subscribe({
      next: () => this.tasks.update(list => list.filter(t => t.id !== id)),
      error: () => alert('Failed to delete task.')
    });
  }

  startCreate(): void {
    this.editingTask.set(null);
    this.showCreateForm.set(true);
  }

  startEdit(task: Task): void {
    this.showCreateForm.set(false);
    this.editingTask.set(task);
  }

  cancelForm(): void {
    this.showCreateForm.set(false);
    this.editingTask.set(null);
    this.formError.set('');
  }
}
