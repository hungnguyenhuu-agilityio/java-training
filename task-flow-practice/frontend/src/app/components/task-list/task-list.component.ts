import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskService } from '../../services/task.service';
import { Task, TaskCreateRequest } from '../../models/task.model';
import { TaskFormComponent } from '../task-form/task-form.component';
import { TaskItemComponent } from '../task-item/task-item.component';

@Component({
  selector: 'app-task-list',
  standalone: true,
  imports: [CommonModule, TaskFormComponent, TaskItemComponent],
  templateUrl: './task-list.component.html'
})
export class TaskListComponent implements OnInit {
  private taskService = inject(TaskService);

  tasks: Task[] = [];
  showCreateForm = false;
  editingTask: Task | null = null;
  loadError = '';
  formError = '';

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loadError = '';
    this.taskService.getTasks().subscribe({
      next: tasks => (this.tasks = tasks),
      error: () => (this.loadError = 'Failed to load tasks. Please try again.')
    });
  }

  onCreate(payload: TaskCreateRequest): void {
    this.formError = '';
    this.taskService.createTask(payload).subscribe({
      next: task => {
        this.tasks = [...this.tasks, task];
        this.showCreateForm = false;
      },
      error: err => {
        this.formError = err?.error?.message ?? 'Failed to create task.';
      }
    });
  }

  onUpdate(payload: TaskCreateRequest): void {
    if (!this.editingTask) return;
    this.formError = '';
    const id = this.editingTask.id;
    this.taskService.updateTask(id, payload).subscribe({
      next: updated => {
        this.tasks = this.tasks.map(t => (t.id === id ? updated : t));
        this.editingTask = null;
      },
      error: err => {
        this.formError = err?.error?.message ?? 'Failed to update task.';
      }
    });
  }

  onDelete(id: number): void {
    this.taskService.deleteTask(id).subscribe({
      next: () => (this.tasks = this.tasks.filter(t => t.id !== id)),
      error: () => alert('Failed to delete task.')
    });
  }

  startCreate(): void {
    this.editingTask = null;
    this.showCreateForm = true;
  }

  startEdit(task: Task): void {
    this.showCreateForm = false;
    this.editingTask = task;
  }

  cancelForm(): void {
    this.showCreateForm = false;
    this.editingTask = null;
    this.formError = '';
  }
}
