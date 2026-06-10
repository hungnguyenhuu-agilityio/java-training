import { Component, Input, Output, EventEmitter, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Task, TaskStatus, PRIORITY_LABELS } from '../../models/task.model';

@Component({
  selector: 'app-task-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-card.component.html',
  styleUrls: ['./task-card.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaskCardComponent {
  @Input() task!: Task;
  @Output() statusChanged = new EventEmitter<{ task: Task; nextStatus: TaskStatus }>();

  readonly moving = signal(false);

  readonly priorityLabels = PRIORITY_LABELS;

  get nextStatus(): TaskStatus | null {
    if (this.task.status === 'TODO') return 'IN_PROGRESS';
    if (this.task.status === 'IN_PROGRESS') return 'DONE';
    return null;
  }

  onMove(): void {
    if (this.moving() || !this.nextStatus) return;
    this.moving.set(true);
    this.statusChanged.emit({ task: this.task, nextStatus: this.nextStatus });
  }

  onReset(): void {
    if (this.moving()) return;
    this.moving.set(true);
    this.statusChanged.emit({ task: this.task, nextStatus: 'TODO' });
  }

  resetMoving(): void {
    this.moving.set(false);
  }
}
