import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Task, TASK_STATUS_LABELS, PRIORITY_LABELS } from '../../models/task.model';

@Component({
  selector: 'app-task-item',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './task-item.component.html'
})
export class TaskItemComponent {
  @Input() task!: Task;
  @Output() editClicked = new EventEmitter<Task>();
  @Output() deleteClicked = new EventEmitter<number>();

  statusLabels = TASK_STATUS_LABELS;
  priorityLabels = PRIORITY_LABELS;

  onEdit(): void {
    this.editClicked.emit(this.task);
  }

  onDelete(): void {
    if (window.confirm(`Delete task "${this.task.title}"?`)) {
      this.deleteClicked.emit(this.task.id);
    }
  }
}
