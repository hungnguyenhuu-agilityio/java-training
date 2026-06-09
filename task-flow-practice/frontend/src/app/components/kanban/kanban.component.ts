import { Component, OnInit, ViewChildren, QueryList } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Task, TaskStatus } from '../../models/task.model';
import { TaskService } from '../../services/task.service';
import { KanbanColumnComponent } from '../kanban-column/kanban-column.component';

@Component({
  selector: 'app-kanban',
  standalone: true,
  imports: [CommonModule, KanbanColumnComponent],
  templateUrl: './kanban.component.html'
})
export class KanbanComponent implements OnInit {
  tasks: Task[] = [];
  errorMessage = '';
  loading = false;

  @ViewChildren(KanbanColumnComponent) columns!: QueryList<KanbanColumnComponent>;

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    this.loading = true;
    this.taskService.getTasks().subscribe({
      next: tasks => {
        this.tasks = tasks;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load tasks.';
        this.loading = false;
      }
    });
  }

  get todoTasks(): Task[] {
    return this.tasks.filter(t => t.status === 'TODO');
  }

  get inProgressTasks(): Task[] {
    return this.tasks.filter(t => t.status === 'IN_PROGRESS');
  }

  get doneTasks(): Task[] {
    return this.tasks.filter(t => t.status === 'DONE');
  }

  onStatusChanged(event: { task: Task; nextStatus: TaskStatus }): void {
    const { task, nextStatus } = event;
    const prevStatus = task.status;

    // Optimistically update in local array
    const idx = this.tasks.findIndex(t => t.id === task.id);
    if (idx === -1) return;
    this.tasks[idx] = { ...this.tasks[idx], status: nextStatus };

    this.taskService.updateTask(task.id, { ...task, status: nextStatus }).subscribe({
      next: updated => {
        this.tasks[idx] = updated;
        // Card moved to a new column — new instance, moving resets naturally
        this.errorMessage = '';
      },
      error: () => {
        // Revert
        this.tasks[idx] = { ...this.tasks[idx], status: prevStatus };
        this.errorMessage = `Failed to update task "${task.title}". Please try again.`;
        // Reset the card's moving flag (it's still in the same column)
        this.columns.forEach(col => col.resetCard(task.id));
      }
    });
  }
}
